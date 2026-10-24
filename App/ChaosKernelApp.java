package App;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ChaosKernelApp {

    static final String S_CPU = "S18_CPU_RUNNING";
    static final String S_CHAOS = "S75_CHAOS_ENGINE_POLL";
    static final String ACTIVE = "PROC_ACTIVE";
    static final String LEAK = "LEAK";
    static final String Z0 = "Z0";

    static final int TOTAL_FRAMES = 64;
    static final int QUANTUM = 3;
    static final int CHAOS_PCT = 15;
    static final int OOM_THRESHOLD_PCT = 95;

    enum State {
        NEW, READY, RUNNING, TERMINATED
    }

    static class PCB {
        final int pid;
        State state = State.READY;
        int priority;
        int cyclesLeft;
        final List<Integer> frames = new ArrayList<>();
        final Deque<String> stack = new ArrayDeque<>();

        PCB(int pid, int priority, int cycles) {
            this.pid = pid;
            this.priority = priority;
            this.cyclesLeft = cycles;
            stack.push(Z0);
            stack.push(ACTIVE);
        }

        String top() {
            return stack.isEmpty() ? "(vacia)" : stack.peek();
        }

        int leakCount() {
            int c = 0;
            for (String s : stack)
                if (LEAK.equals(s))
                    c++;
            return c;
        }
    }

    static class Ram {
        final boolean[] used = new boolean[TOTAL_FRAMES];

        int freeCount() {
            int c = 0;
            for (boolean b : used)
                if (!b)
                    c++;
            return c;
        }

        int usedCount() {
            return TOTAL_FRAMES - freeCount();
        }

        List<Integer> alloc(int n) {
            if (n > freeCount())
                return null;
            List<Integer> res = new ArrayList<>();
            for (int i = 0; i < TOTAL_FRAMES && res.size() < n; i++) {
                if (!used[i]) {
                    used[i] = true;
                    res.add(i);
                }
            }
            return res;
        }

        void release(List<Integer> fr) {
            for (int i : fr)
                used[i] = false;
            fr.clear();
        }
    }

    static class Kernel {
        final Ram ram = new Ram();
        final List<PCB> all = new ArrayList<>();
        final Deque<PCB> ready = new ArrayDeque<>();
        final Random rnd = new Random();
        final java.util.function.Consumer<String> logger;
        PCB running = null;
        int quantumLeft = 0;
        int nextPid = 1;
        int clock = 0;

        Kernel(java.util.function.Consumer<String> logger) {
            this.logger = logger;
        }

        void log(String s) {
            logger.accept("[t=" + clock + "] " + s);
        }

        void newProcess() {
            int need = 2 + rnd.nextInt(5);
            List<Integer> fr = ram.alloc(need);
            if (fr == null) {
                log("delta(NEW, spawn, Z0) -> RECHAZADO: RAM insuficiente (" + need
                        + " marcos pedidos, " + ram.freeCount() + " libres)");
                return;
            }
            PCB p = new PCB(nextPid++, 1 + rnd.nextInt(5), 5 + rnd.nextInt(11));
            p.frames.addAll(fr);
            all.add(p);
            ready.addLast(p);
            log("delta(NEW, spawn, Z0) -> (READY, " + ACTIVE + "." + Z0 + ") PID=" + p.pid
                    + " marcos=" + p.frames.size() + " ciclos=" + p.cyclesLeft + " prio=" + p.priority);
        }

        void tick() {
            clock++;
            if (running == null) {
                running = ready.pollFirst();
                if (running == null) {
                    log("delta(" + S_CPU + ", tick, -) -> CPU ociosa, cola de listos vacia");
                    checkOom();
                    return;
                }
                running.state = State.RUNNING;
                quantumLeft = QUANTUM;
                log("DISPATCH PID=" + running.pid + " -> " + S_CPU);
            }
            PCB p = running;
            if (ACTIVE.equals(p.top()) && rnd.nextInt(100) < CHAOS_PCT) {
                log("delta(" + S_CPU + ", tick, " + ACTIVE + ") -> (" + S_CHAOS + ", " + ACTIVE
                        + ") bifurcacion no determinista PID=" + p.pid);
                chaosOn(p, false);
                checkOom();
                return;
            }
            p.cyclesLeft--;
            quantumLeft--;
            log("delta(" + S_CPU + ", tick, " + p.top() + ") -> (" + S_CPU + ", " + p.top()
                    + ") PID=" + p.pid + " ciclos restantes=" + p.cyclesLeft);
            if (p.cyclesLeft <= 0) {
                int popped = p.stack.size();
                p.stack.clear();
                ram.release(p.frames);
                p.state = State.TERMINATED;
                running = null;
                log("delta(" + S_CPU + ", exit, " + ACTIVE + ") -> (TERMINATED, e) PID=" + p.pid
                        + " pop=" + popped + " RAM liberada");
            } else if (quantumLeft <= 0) {
                p.state = State.READY;
                ready.addLast(p);
                running = null;
                log("QUANTUM agotado PID=" + p.pid + " -> READY (Round Robin)");
            }
            checkOom();
        }

        void chaosOn(PCB p, boolean saturate) {
            int want = saturate ? ram.freeCount() : 4 + rnd.nextInt(7);
            int got = Math.min(want, ram.freeCount());
            List<Integer> fr = ram.alloc(got);
            if (fr != null)
                p.frames.addAll(fr);
            String prevTop = p.top();
            p.stack.push(LEAK);
            log("delta(" + S_CHAOS + ", fault, " + prevTop + ") -> (READY, " + LEAK + "."
                    + prevTop + ") PID=" + p.pid + " fuga=" + got + " marcos, RAM libre="
                    + ram.freeCount());
            if (p == running)
                running = null;
            if (p.state != State.TERMINATED) {
                p.state = State.READY;
                if (!ready.contains(p))
                    ready.addLast(p);
            }
        }

        void injectChaos() {
            clock++;
            List<PCB> live = livePcbs();
            if (live.isEmpty()) {
                log("INYECTAR CAOS: no hay procesos vivos");
                return;
            }
            PCB target = (running != null) ? running : live.get(rnd.nextInt(live.size()));
            log("INYECCION DE CAOS manual -> " + S_CHAOS + " PID=" + target.pid);
            chaosOn(target, true);
            checkOom();
        }

        void checkOom() {
            int pct = ram.usedCount() * 100 / TOTAL_FRAMES;
            if (pct >= OOM_THRESHOLD_PCT) {
                log("RAM al " + pct + "% (>= " + OOM_THRESHOLD_PCT + "%): se dispara OOM Killer automatico");
                oomKill();
            }
        }

        void oomKill() {
            List<PCB> live = livePcbs();
            if (live.isEmpty()) {
                log("OOM Killer: sin procesos que evaluar");
                return;
            }
            PCB victim = null;
            int best = Integer.MIN_VALUE;
            for (PCB p : live) {
                int score = p.frames.size() + 20 * p.leakCount() - p.priority;
                if (score > best) {
                    best = score;
                    victim = p;
                }
            }
            int popped = 0;
            while (!victim.stack.isEmpty()) {
                victim.stack.pop();
                popped++;
            }
            int freed = victim.frames.size();
            ram.release(victim.frames);
            victim.state = State.TERMINATED;
            ready.remove(victim);
            if (victim == running)
                running = null;
            log("OOM KILLER: victima PID=" + victim.pid + " score=" + best + " | pop total=" + popped
                    + " simbolos | " + freed + " marcos liberados | -> TERMINATED, pila=(vacia) | RAM libre="
                    + ram.freeCount());
        }

        List<PCB> livePcbs() {
            List<PCB> l = new ArrayList<>();
            for (PCB p : all)
                if (p.state != State.TERMINATED)
                    l.add(p);
            return l;
        }
    }

    JFrame frame;
    DefaultTableModel model;
    JProgressBar ramBar;
    JTextArea console;
    Kernel kernel;

    void build() {
        kernel = new Kernel(this::appendLog);

        frame = new JFrame("ChaosKernel-SIM - APND Microkernel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(6, 6));

        ramBar = new JProgressBar(0, TOTAL_FRAMES);
        ramBar.setStringPainted(true);
        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createTitledBorder("RAM fisica (marcos)"));
        north.add(ramBar, BorderLayout.CENTER);
        frame.add(north, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new Object[] { "PID", "Estado", "Prioridad", "Ciclos", "Marcos", "Cima de pila", "Prof. pila" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(model);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Procesos (PCB)"));

        console = new JTextArea(12, 80);
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane consoleScroll = new JScrollPane(console);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Consola de eventos / transiciones delta"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, consoleScroll);
        split.setResizeWeight(0.5);
        frame.add(split, BorderLayout.CENTER);

        JButton bNew = new JButton("Nuevo Proceso");
        JButton bTick = new JButton("Tick Reloj");
        JButton bChaos = new JButton("Inyectar Caos");
        JButton bOom = new JButton("OOM Killer");
        bNew.addActionListener(e -> {
            kernel.newProcess();
            refresh();
        });
        bTick.addActionListener(e -> {
            kernel.tick();
            refresh();
        });
        bChaos.addActionListener(e -> {
            kernel.injectChaos();
            refresh();
        });
        bOom.addActionListener(e -> {
            kernel.clock++;
            kernel.oomKill();
            refresh();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttons.add(bNew);
        buttons.add(bTick);
        buttons.add(bChaos);
        buttons.add(bOom);
        frame.add(buttons, BorderLayout.SOUTH);

        appendLog("ChaosKernel-SIM iniciado. Estado central: " + S_CPU + ", cima: " + ACTIVE);
        refresh();

        frame.setSize(900, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    void appendLog(String s) {
        console.append(s + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }

    void refresh() {
        model.setRowCount(0);
        for (PCB p : kernel.all) {
            model.addRow(new Object[] {
                    p.pid, p.state, p.priority, p.cyclesLeft, p.frames.size(), p.top(), p.stack.size()
            });
        }
        int used = kernel.ram.usedCount();
        int pct = used * 100 / TOTAL_FRAMES;
        ramBar.setValue(used);
        ramBar.setString(used + " / " + TOTAL_FRAMES + " marcos (" + pct + "%)");
        ramBar.setForeground(pct >= 80 ? Color.RED : pct >= 50 ? Color.ORANGE : new Color(0, 150, 0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChaosKernelApp().build());
    }
}