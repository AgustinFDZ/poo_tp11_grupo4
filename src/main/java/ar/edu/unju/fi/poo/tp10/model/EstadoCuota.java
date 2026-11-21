package ar.edu.unju.fi.poo.tp10.model;

/**
 * Estados de una cuota.
 * <ul>
 *   <li>{@code ADEUDADA}: pendiente; puede abonarse hasta su fecha de vencimiento (inclusive) por su monto original.</li>
 *   <li>{@code VENCIDA}: pendiente cuya fecha de vencimiento ya pasó (se actualiza sola al consultar o cobrar).</li>
 *   <li>{@code PAGADA}: ya fue abonada.</li>
 * </ul>
 */
public enum EstadoCuota { ADEUDADA, VENCIDA, PAGADA }
