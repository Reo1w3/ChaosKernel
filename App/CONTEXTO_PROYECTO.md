# ChaosKernel-SIM - Especificaciones del Proyecto

## 1. Naturaleza y Objetivo
Simulador interactivo de microkernel en Java 17+ (Swing nativo, sin dependencias externas) modelado formalmente como un Autómata de Pila No Determinista (APND) de 105 estados verificado en JFLAP.

## 2. Modelo Teórico (APND)
- Estado central en CPU: S18_CPU_RUNNING
- Cima de Pila (Gamma): PROC_ACTIVE
- No Determinismo: Con el evento "tick" y cima "PROC_ACTIVE", el sistema puede:
  a) Continuar ejecución normal en S18.
  b) Bifurcar a S75_CHAOS_ENGINE_POLL (anomalía asíncrona / inyección de fallos).
- Defensas del Kernel: El OOM Killer evalúa procesos y realiza un pop total de la pila formal (vaciado), liberando marcos de RAM físicos y pasando el proceso a TERMINATED.

## 3. Arquitectura del Código
- Modelo de Proceso: PCB (PID, estado, prioridad, ciclos restantes, marcos de RAM asignados, Pila formal ArrayDeque<String>).
- Memoria Física: Arreglo booleano o bitmap de marcos de RAM físicos.
- Planificador: Cola de listos FIFO/Round Robin con avance por ciclo "tick".
- Interfaz Gráfica (Swing):
  * JTable con los procesos y la CIMA de su pila formal.
  * JProgressBar que muestra el uso de RAM física.
  * JTextArea para consola de eventos/transiciones delta.
  * Botones: [Nuevo Proceso], [Tick Reloj], [Inyectar Caos], [OOM Killer].

## 4. Regla de Ahorro para el Asistente
El usuario usa el tier gratuito. Sé ultra conciso: entrega siempre código Java compilable y completo, sin introducciones, sin saludos, sin explicaciones teóricas ni conclusiones.