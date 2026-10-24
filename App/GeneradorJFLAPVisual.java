package App;

import java.io.FileWriter;
import java.io.IOException;

public class GeneradorJFLAPVisual {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<structure>\n");
        sb.append("  <type>pda</type>\n");
        sb.append("  <automaton>\n");

        String[] stateNames = {
                // S1: Bootstrap (0-9)
                "S00_COLD_BOOT", "S01_ROM_BIOS_CHECK", "S02_CPU_MSR_INIT", "S03_RAM_SCAN_MAP",
                "S04_IVT_LOAD", "S05_APIC_INIT", "S06_PIT_CALIBRATE", "S07_DMA_RESET",
                "S08_PCI_ENUM", "S09_KERNEL_START",
                // S2: Procesos y Contexto (10-21)
                "S10_PROC_NEW", "S11_PCB_ALLOC", "S12_STACK_INIT", "S13_HEAP_INIT",
                "S14_READY_Q_HIGH", "S15_READY_Q_MED", "S16_READY_Q_LOW", "S17_CPU_DISPATCH",
                "S18_CPU_RUNNING", "S19_PREEMPTED", "S20_CTX_SAVE", "S21_CTX_RESTORE",
                // S3: Planificador MLFQ (22-31)
                "S22_SCHED_TICK", "S23_EVAL_BURST", "S24_QUANTUM_HIGH_EXPIRED", "S25_QUANTUM_MED_EXPIRED",
                "S26_QUANTUM_LOW_EXPIRED", "S27_MLFQ_DEMOTE", "S28_AGING_CHECK", "S29_MLFQ_PROMOTE",
                "S30_STARVATION_RESCUE", "S31_AFFINITY_BIND",
                // S4: Memoria Física & Paginación (32-43)
                "S32_MEM_REQ", "S33_TLB_PROBE", "S34_TLB_HIT", "S35_TLB_MISS",
                "S36_CR3_LOOKUP", "S37_PAGE_WALK", "S38_PAGE_FAULT_EXC", "S39_FRAME_ALLOC",
                "S40_FRAME_EXHAUSTED", "S41_LRU_PROBE", "S42_PAGE_DIRTY_EVICT", "S43_PAGE_MAP_UPDATE",
                // S5: Paginador Swap & DMA (44-53)
                "S44_SWAP_QUEUE_IN", "S45_DISK_SEEK_REQ", "S46_DMA_XFER_START", "S47_DMA_BUSY_WAIT",
                "S48_SWAP_SECTOR_WRITE", "S49_SWAP_SECTOR_READ", "S50_BUFFER_CACHE_FILL", "S51_FLUSH_SWAP_BITMAP",
                "S52_SWAP_IO_COMPLETED", "S53_UNBLOCK_PROC_MEM",
                // S6: Concurrencia y Cerrojos (54-64)
                "S54_MUTEX_ACQUIRE", "S55_CAS_ATOMIC_EVAL", "S56_LOCK_GRANTED", "S57_LOCK_COLLISION",
                "S58_SPIN_RETRY", "S59_FUTEX_SUSPEND", "S60_WAIT_QUEUE_INSERT", "S61_MUTEX_RELEASE",
                "S62_SEM_POST", "S63_WAKE_HEAD_THREAD", "S64_BARRIER_WAIT_SYNC",
                // S7: Detección Deadlocks (65-74)
                "S65_RESOURCE_REQ", "S66_BANKER_SAFE_CHECK", "S67_BANKER_UNSAFE_DENY", "S68_RAG_UPDATE_EDGE",
                "S69_TARJAN_DFS_CYCLE", "S70_CYCLE_DETECTED", "S71_EVAL_VICTIM_PENALTY", "S72_PREEMPT_MUTEX_HOLD",
                "S73_ROLLBACK_STATE", "S74_RAG_REMOVE_EDGE",
                // S8: Motor de Caos (75-84)
                "S75_CHAOS_ENGINE_POLL", "S76_INJECT_FORK_BOMB", "S77_INJECT_RAM_LEAK", "S78_INJECT_DIRTY_DEADLOCK",
                "S79_INJECT_STACK_SMASH", "S80_INJECT_BIT_ROT", "S81_INJECT_THRASH_ATTACK", "S82_INJECT_FD_DEPLETION",
                "S83_INJECT_CPU_MONOPOLY", "S84_INJECT_NULL_DEREF",
                // S9: Defensas & OOM Killer (85-94)
                "S85_DEFENSE_GATEWAY", "S86_EVAL_OOM_BADNESS", "S87_OOM_KILL_SIGNAL", "S88_FORK_RATE_LIMITER",
                "S89_MEMORY_QUOTA_STRIP", "S90_WATCHDOG_INTERRUPT", "S91_CORRUPT_FRAME_ISOLATE", "S92_THROTTLE_PROCS",
                "S93_ZOMBIE_REAPER_SWEEP", "S94_SYS_RESTABILIZE",
                // S10: Forense y Salida (95-104)
                "S95_FLIGHT_REC_LOG", "S96_SNAPSHOT_PAGE_TABLES", "S97_DUMP_REGISTERS", "S98_CORE_DUMP_EXPORT",
                "S99_ROOT_CAUSE_ISOLATE", "S100_PROC_ZOMBIE", "S101_PROC_TERMINATED", "S102_RESOURCE_FREE_ALL",
                "S103_KERNEL_HALT_PANIC", "S104_CLEAN_SHUTDOWN"
        };

        // Layout estructurado en bloques de subsistemas para máxima legibilidad
        for (int i = 0; i < stateNames.length; i++) {
            int subSystem = getSubSystem(i);
            int idxInSub = getIndexInSubSystem(i);

            // Coordenadas con espacio amplio para que las etiquetas no se crucen
            int x = 200 + (subSystem % 3) * 1600 + (idxInSub % 4) * 380;
            int y = 150 + (subSystem / 3) * 1100 + (idxInSub / 4) * 320;

            // Destacar nodo central S18 en una posición visible
            if (i == 18) {
                x = 1800;
                y = 800;
            }

            sb.append("    <state id=\"").append(i).append("\" name=\"").append(stateNames[i]).append("\">\n");
            sb.append("      <x>").append(x).append("</x><y>").append(y).append("</y>\n");
            if (i == 0)
                sb.append("      <initial/>\n");
            if (i == 103 || i == 104)
                sb.append("      <final/>\n");
            sb.append("    </state>\n");
        }

        String[][] transitions = {
                { "0", "1", "pwr_on", "Z", "FRAME_ROM" },
                { "1", "2", "rom_ok", "FRAME_ROM", "FRAME_MSR" },
                { "2", "3", "msr_ok", "FRAME_MSR", "FRAME_RAM" },
                { "3", "4", "ram_ok", "FRAME_RAM", "FRAME_IVT" },
                { "4", "5", "ivt_ok", "FRAME_IVT", "FRAME_APIC" },
                { "5", "6", "apic_ok", "FRAME_APIC", "FRAME_PIT" },
                { "6", "7", "pit_ok", "FRAME_PIT", "FRAME_DMA" },
                { "7", "8", "dma_ok", "FRAME_DMA", "FRAME_PCI" },
                { "8", "9", "pci_ok", "FRAME_PCI", "K_INIT" },
                { "9", "10", "spawn_init", "K_INIT", "PCB_INIT K_INIT" },

                { "10", "11", "pcb_req", "PCB_INIT", "PCB_SLOT" },
                { "11", "12", "stack_req", "PCB_SLOT", "STACK_FRAME" },
                { "12", "13", "heap_req", "STACK_FRAME", "USER_STACK" },
                { "13", "14", "push_high", "USER_STACK", "Q_HIGH" },
                { "13", "15", "push_med", "USER_STACK", "Q_MED" },
                { "13", "16", "push_low", "USER_STACK", "Q_LOW" },
                { "14", "17", "sched_pop", "Q_HIGH", "DISPATCH_TOKEN" },
                { "15", "17", "sched_pop", "Q_MED", "DISPATCH_TOKEN" },
                { "16", "17", "sched_pop", "Q_LOW", "DISPATCH_TOKEN" },
                { "17", "18", "context_in", "DISPATCH_TOKEN", "PROC_ACTIVE" },
                { "18", "19", "preempt_tick", "PROC_ACTIVE", "PREEMPT_TOKEN" },
                { "19", "20", "save_regs", "PREEMPT_TOKEN", "SAVED_CTX" },
                { "20", "21", "switch_out", "SAVED_CTX", "RESTORE_TOKEN" },
                { "21", "18", "context_in", "RESTORE_TOKEN", "PROC_ACTIVE" },

                { "18", "22", "sched_eval", "PROC_ACTIVE", "PROC_ACTIVE" },
                { "22", "23", "eval_slice", "PROC_ACTIVE", "BURST_METRIC" },
                { "23", "24", "slice_high_end", "BURST_METRIC", "DEMOTE_HIGH" },
                { "23", "25", "slice_med_end", "BURST_METRIC", "DEMOTE_MED" },
                { "23", "26", "slice_low_end", "BURST_METRIC", "REQUEUE_LOW" },
                { "24", "27", "apply_demote", "DEMOTE_HIGH", "Q_MED" },
                { "25", "27", "apply_demote", "DEMOTE_MED", "Q_LOW" },
                { "26", "16", "push_back", "REQUEUE_LOW", "Q_LOW" },
                { "27", "28", "audit_aging", "Q_MED", "AGING_METRIC" },
                { "28", "29", "promote_proc", "AGING_METRIC", "Q_HIGH" },
                { "28", "30", "starve_detected", "AGING_METRIC", "FORCE_RUN" },
                { "30", "18", "grant_cpu", "FORCE_RUN", "PROC_ACTIVE" },
                { "29", "31", "bind_cpu", "Q_HIGH", "AFFINITY_CORE" },
                { "31", "14", "requeue_high", "AFFINITY_CORE", "Q_HIGH" },

                { "18", "32", "sys_mem_access", "PROC_ACTIVE", "MEM_PROBE PROC_ACTIVE" },
                { "32", "33", "probe_tlb", "MEM_PROBE", "TLB_TAG" },
                { "33", "34", "hit", "TLB_TAG", "PHYS_ADDR" },
                { "34", "18", "resume_exec", "PHYS_ADDR", "" },
                { "33", "35", "miss", "TLB_TAG", "CR3_LOOKUP" },
                { "35", "36", "lookup_dir", "CR3_LOOKUP", "PTE_PTR" },
                { "36", "37", "walk_table", "PTE_PTR", "PAGE_STATUS" },
                { "37", "34", "page_present", "PAGE_STATUS", "PHYS_ADDR" },
                { "37", "38", "page_absent", "PAGE_STATUS", "SIG_SEGV" },
                { "38", "39", "try_alloc_frame", "SIG_SEGV", "FRAME_REQ" },
                { "39", "43", "alloc_success", "FRAME_REQ", "PAGE_MAPPED" },
                { "39", "40", "ram_depleted", "FRAME_REQ", "OOM_EVICT" },
                { "40", "41", "eval_lru", "OOM_EVICT", "LRU_TARGET" },
                { "41", "42", "evict_victim", "LRU_TARGET", "FLUSH_FRAME" },
                { "42", "44", "queue_swap", "FLUSH_FRAME", "SWAP_REQ" },
                { "43", "34", "map_updated", "PAGE_MAPPED", "PHYS_ADDR" },

                { "44", "45", "seek_sector", "SWAP_REQ", "IO_PENDING" },
                { "45", "46", "trigger_dma", "IO_PENDING", "DMA_XFER" },
                { "46", "47", "transferring", "DMA_XFER", "DMA_BUSY" },
                { "47", "48", "write_block", "DMA_BUSY", "SECTOR_WRITTEN" },
                { "47", "49", "read_block", "DMA_BUSY", "SECTOR_READ" },
                { "48", "51", "sync_bitmap", "SECTOR_WRITTEN", "BITMAP_UPDATED" },
                { "49", "50", "cache_fill", "SECTOR_READ", "CACHE_READY" },
                { "51", "52", "io_done", "BITMAP_UPDATED", "IO_COMPLETED" },
                { "50", "52", "io_done", "CACHE_READY", "IO_COMPLETED" },
                { "52", "53", "unblock_thread", "IO_COMPLETED", "THREAD_READY" },
                { "53", "14", "requeue_ready", "THREAD_READY", "Q_HIGH" },

                { "18", "54", "mutex_lock", "PROC_ACTIVE", "LOCK_REQ PROC_ACTIVE" },
                { "54", "55", "eval_cas", "LOCK_REQ", "CAS_EVAL" },
                { "55", "56", "unlocked", "CAS_EVAL", "OWN_LOCK" },
                { "56", "18", "lock_granted", "OWN_LOCK", "" },
                { "55", "57", "collision", "CAS_EVAL", "WAIT_LOCK" },
                { "57", "58", "spin_short", "WAIT_LOCK", "SPIN_COUNT" },
                { "58", "55", "retry_cas", "SPIN_COUNT", "CAS_EVAL" },
                { "58", "59", "spin_exhausted", "SPIN_COUNT", "FUTEX_CALL" },
                { "59", "60", "park_thread", "FUTEX_CALL", "BLOCKED_LOCK" },
                { "60", "65", "audit_lock_graph", "BLOCKED_LOCK", "AUDIT_RAG" },
                { "18", "61", "mutex_unlock", "PROC_ACTIVE", "RELEASE_REQ PROC_ACTIVE" },
                { "61", "62", "post_signal", "RELEASE_REQ", "SIG_POST" },
                { "62", "63", "wake_one", "SIG_POST", "UNPARK_TOKEN" },
                { "63", "14", "requeue_unblocked", "UNPARK_TOKEN", "Q_HIGH" },
                { "18", "64", "barrier_sync", "PROC_ACTIVE", "BARRIER_WAIT" },
                { "64", "63", "barrier_cleared", "BARRIER_WAIT", "UNPARK_TOKEN" },

                { "65", "66", "banker_test", "AUDIT_RAG", "BANKER_SIM" },
                { "66", "68", "state_safe", "BANKER_SIM", "EDGE_INSERT" },
                { "66", "67", "state_unsafe", "BANKER_SIM", "DENY_ALLOC" },
                { "67", "60", "wait_release", "DENY_ALLOC", "BLOCKED_LOCK" },
                { "68", "69", "run_tarjan", "EDGE_INSERT", "CYCLE_SEARCH" },
                { "69", "74", "no_cycle", "CYCLE_SEARCH", "SAFE_RETURN" },
                { "74", "18", "resume_normal", "SAFE_RETURN", "" },
                { "69", "70", "cycle_found", "CYCLE_SEARCH", "DEADLOCK_PANIC" },
                { "70", "71", "rank_penalties", "DEADLOCK_PANIC", "PICK_VICTIM" },
                { "71", "72", "preempt_held", "PICK_VICTIM", "STRIP_LOCKS" },
                { "72", "73", "rollback_pcb", "STRIP_LOCKS", "RESET_FRAME" },
                { "73", "10", "restart_proc", "RESET_FRAME", "PCB_INIT" },

                // BIFURCACIÓN NO DETERMINISTA
                { "18", "18", "tick", "PROC_ACTIVE", "PROC_ACTIVE" },
                { "18", "75", "tick", "PROC_ACTIVE", "CHAOS_POLL PROC_ACTIVE" },

                { "75", "76", "chaos_fork", "CHAOS_POLL", "ATTACK_FORKBOMB" },
                { "75", "77", "chaos_leak", "CHAOS_POLL", "ATTACK_RAMLEAK" },
                { "75", "78", "chaos_deadlock", "CHAOS_POLL", "ATTACK_DLOCK" },
                { "75", "79", "chaos_smash", "CHAOS_POLL", "ATTACK_STACK" },
                { "75", "80", "chaos_bitflip", "CHAOS_POLL", "ATTACK_BITROT" },
                { "75", "81", "chaos_thrash", "CHAOS_POLL", "ATTACK_THRASH" },
                { "75", "82", "chaos_fd", "CHAOS_POLL", "ATTACK_FDLEAK" },
                { "75", "83", "chaos_hog", "CHAOS_POLL", "ATTACK_CPUMONOPOLY" },
                { "75", "84", "chaos_null", "CHAOS_POLL", "ATTACK_NULLPTR" },

                { "76", "85", "raise_alarm", "ATTACK_FORKBOMB", "GATEWAY_ALARM" },
                { "77", "85", "raise_alarm", "ATTACK_RAMLEAK", "GATEWAY_ALARM" },
                { "78", "85", "raise_alarm", "ATTACK_DLOCK", "GATEWAY_ALARM" },
                { "79", "85", "raise_alarm", "ATTACK_STACK", "GATEWAY_ALARM" },
                { "80", "85", "raise_alarm", "ATTACK_BITROT", "GATEWAY_ALARM" },
                { "81", "85", "raise_alarm", "ATTACK_THRASH", "GATEWAY_ALARM" },
                { "82", "85", "raise_alarm", "ATTACK_FDLEAK", "GATEWAY_ALARM" },
                { "83", "85", "raise_alarm", "ATTACK_CPUMONOPOLY", "GATEWAY_ALARM" },
                { "84", "85", "raise_alarm", "ATTACK_NULLPTR", "GATEWAY_ALARM" },

                { "85", "86", "eval_badness", "GATEWAY_ALARM", "RUN_OOM_METRIC" },
                { "86", "87", "sig_kill_oom", "RUN_OOM_METRIC", "TERMINATE_PROC" },
                { "85", "88", "clamp_fork", "GATEWAY_ALARM", "QUOTA_FORK_APPLIED" },
                { "85", "89", "strip_pages", "GATEWAY_ALARM", "RECLAIM_RAM" },
                { "85", "90", "wd_timeout", "GATEWAY_ALARM", "NMI_FIRE" },
                { "85", "91", "isolate_frame", "GATEWAY_ALARM", "QUARANTINE_RAM" },
                { "85", "92", "throttle_procs", "GATEWAY_ALARM", "APPLY_SLEEP_PENALTY" },
                { "85", "93", "sweep_zombies", "GATEWAY_ALARM", "ZOMBIE_FLUSH" },
                { "88", "94", "restore_ok", "QUOTA_FORK_APPLIED", "SYSTEM_STABLE" },
                { "89", "94", "restore_ok", "RECLAIM_RAM", "SYSTEM_STABLE" },
                { "91", "94", "restore_ok", "QUARANTINE_RAM", "SYSTEM_STABLE" },
                { "92", "94", "restore_ok", "APPLY_SLEEP_PENALTY", "SYSTEM_STABLE" },
                { "93", "94", "restore_ok", "ZOMBIE_FLUSH", "SYSTEM_STABLE" },
                { "94", "18", "return_cpu", "SYSTEM_STABLE", "" },

                { "87", "95", "record_kill", "TERMINATE_PROC", "BLACKBOX_RECORD" },
                { "90", "95", "record_nmi", "NMI_FIRE", "BLACKBOX_RECORD" },
                { "95", "96", "snapshot_pt", "BLACKBOX_RECORD", "PT_DUMP" },
                { "96", "97", "snapshot_regs", "PT_DUMP", "REG_DUMP" },
                { "97", "98", "export_dump", "REG_DUMP", "CORE_FILE_EMITTED" },
                { "98", "99", "audit_cause", "CORE_FILE_EMITTED", "LOG_ANALYZED" },
                { "99", "103", "kernel_unrecoverable", "LOG_ANALYZED", "HALT_PANIC" },

                { "18", "100", "sys_exit", "PROC_ACTIVE", "EXIT_STATUS" },
                { "100", "101", "parent_wait", "EXIT_STATUS", "CLEANUP_TOKEN" },
                { "101", "102", "release_resources", "CLEANUP_TOKEN", "RESOURCES_FREED" },
                { "102", "9", "return_idle", "RESOURCES_FREED", "K_INIT" },
                { "9", "104", "sys_poweroff", "K_INIT", "SHUTDOWN_OK" }
        };

        for (String[] tr : transitions) {
            sb.append("    <transition>\n");
            sb.append("      <from>").append(tr[0]).append("</from>\n");
            sb.append("      <to>").append(tr[1]).append("</to>\n");
            sb.append("      <read>").append(tr[2]).append("</read>\n");
            sb.append("      <pop>").append(tr[3]).append("</pop>\n");
            sb.append("      <push>").append(tr[4]).append("</push>\n");
            sb.append("    </transition>\n");
        }

        sb.append("  </automaton>\n");
        sb.append("</structure>\n");

        try (FileWriter fw = new FileWriter("ChaosKernel_APND.jff")) {
            fw.write(sb.toString());
            System.out.println("ChaosKernel_APND.jff regenerado con arquitectura visual por módulos.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getSubSystem(int id) {
        if (id <= 9)
            return 0; // S1 Bootstrap
        if (id <= 21)
            return 1; // S2 Ciclo de Vida
        if (id <= 31)
            return 2; // S3 Scheduler
        if (id <= 43)
            return 3; // S4 Memoria
        if (id <= 53)
            return 4; // S5 Swap
        if (id <= 64)
            return 5; // S6 Cerrojos
        if (id <= 74)
            return 6; // S7 Deadlocks
        if (id <= 84)
            return 7; // S8 Caos
        if (id <= 94)
            return 8; // S9 Defensas
        return 9; // S10 Forense
    }

    private static int getIndexInSubSystem(int id) {
        int[] starts = { 0, 10, 22, 32, 44, 54, 65, 75, 85, 95 };
        return id - starts[getSubSystem(id)];
    }
}