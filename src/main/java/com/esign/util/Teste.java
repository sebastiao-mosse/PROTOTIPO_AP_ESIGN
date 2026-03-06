package com.esign.util;

import java.util.ArrayList;
import java.util.List;

public class Teste {

    public static void main(String[]args) 
    {
        String str= "SEBASTIAO VUNDA MOSSE";
        System.out.println("Resultado: "+ getSplitedSelectedValue(str));
    }
    
    public static String getSplitedSelectedValue(String str)
    {
        List<String> lista = new ArrayList<String>();
        String[] separador = str.split(" ");
        for(int i=0; i< separador.length; i++)
        {
            lista.add(separador[i]);
        }
        
        return lista.get(1)+ "";
    }
}
