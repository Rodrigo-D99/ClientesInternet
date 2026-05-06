package com.clientesinternet.util;

public class NumeroALetras {

    private static final String[] UNIDADES = {"", "UN ", "DOS ", "TRES ", "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE "};
    private static final String[] DECENAS = {"DIEZ ", "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS ",
            "DIECISIETE ", "DIECIOCHO ", "DIECINUEVE ", "VEINTE ", "TREINTA ", "CUARENTA ",
            "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA "};
    private static final String[] CENTENAS = {"", "CIENTO ", "DOSCIENTOS ", "TRESCIENTOS ", "CUATROCIENTOS ", "QUINIENTOS ", "SEISCIENTOS ",
            "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "};

    public static String convertir(Double numero) {
        long entero = numero.longValue();
        if (entero == 0) return "CERO";
        return convertirNumero(entero).trim();
    }

    private static String convertirNumero(long numero) {
        if (numero < 10) return UNIDADES[(int) numero];
        if (numero < 20) return DECENAS[(int) (numero - 10)];
        if (numero < 30) return numero == 20 ? "VEINTE " : "VEINTI" + UNIDADES[(int) (numero - 20)];
        if (numero < 100) return DECENAS[(int) (numero / 10) + 8] + (numero % 10 != 0 ? "Y " + convertirNumero(numero % 10) : "");
        if (numero < 1000) return numero == 100 ? "CIEN " : CENTENAS[(int) (numero / 100)] + convertirNumero(numero % 100);
        if (numero < 2000) return "MIL " + convertirNumero(numero % 1000);
        if (numero < 1000000) return convertirNumero(numero / 1000) + "MIL " + convertirNumero(numero % 1000);
        if (numero < 2000000) return "UN MILLON " + convertirNumero(numero % 1000000);
        return convertirNumero(numero / 1000000) + "MILLONES " + convertirNumero(numero % 1000000);
    }
}