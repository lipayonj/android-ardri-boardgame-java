package com.thesis.ArdRi.models;

import java.util.HashMap;
import java.util.Map;

public class BoardCfg {

    public static int BLACK = -1;
    public static int WHITE = -2;
    public static int KING = -3;

    /** Scottish variant of Ard-Ri, translated as "High King" */
    public static final int[] ARD_RI =
    {
        7, // Ard-Ri is played on a 7x7 board
        
        /* Attackers */
        6, 2, BLACK,
        6, 3, BLACK,
        6, 4, BLACK,
        5, 3, BLACK,

        0, 2, BLACK,
        0, 3, BLACK,
        0, 4, BLACK,
        1, 3, BLACK,

        2, 0, BLACK,
        3, 0, BLACK,
        4, 0, BLACK,
        3, 1, BLACK,

        2, 6, BLACK,
        3, 6, BLACK,
        4, 6, BLACK,
        3, 5, BLACK,

        /* Defenders */
        2, 2, WHITE,
        2, 3, WHITE,
        2, 4, WHITE,
        4, 2, WHITE,
        4, 3, WHITE,
        4, 4, WHITE,
        3, 2, WHITE,
        3, 4, WHITE,

        /* King */
        3, 3, KING,
    };

    /** Finnish variant of Tablut */
    protected static final int[] TABLUT =
    {
        9, // Tablut is played on an 9x9 board

        /* Attackers */
        8, 3, BLACK,
        8, 4, BLACK,
        8, 5, BLACK,
        7, 4, BLACK,

        0, 3, BLACK,
        0, 4, BLACK,
        0, 5, BLACK,
        1, 4, BLACK,

        3, 0, BLACK,
        4, 0, BLACK,
        5, 0, BLACK,
        4, 1, BLACK,

        3, 8, BLACK,
        4, 8, BLACK,
        5, 8, BLACK,
        4, 7, BLACK,

        /* Defenders */
        4, 2, WHITE,
        4, 3, WHITE,
        2, 4, WHITE,
        3, 4, WHITE,
        4, 5, WHITE,
        4, 6, WHITE,
        5, 4, WHITE,
        6, 4, WHITE,

        /* King */
        4, 4, KING,
    };

    /** Modern variant of my own devising */
    protected static final int[] ERRK =
    {
        7, // Errk is played on an 7x7 board

        /* Attackers */
        1, 1, BLACK,
        2, 1, BLACK,

        1, 4, BLACK,
        1, 5, BLACK,

        5, 5, BLACK,
        4, 5, BLACK,

        5, 1, BLACK,
        5, 2, BLACK,

        /* Defenders */
        2, 3, WHITE,
        4, 3, WHITE,
        3, 2, WHITE,
        3, 4, WHITE,

        /* King */
        3, 3, KING,
    };
    
    /**
     * Returns the board configuration matching the given name. If no such
     * configuration exists, this method returns the default Ard-Ri config.
     */
    public static int[] getConfiguration(String name) {
        int[] config = _configs.get(name);
        return config != null ? config : ARD_RI;
    }
    
    /** Mapping from configuration name to configuration. */
    protected static Map<String, int[]> _configs;
    
    static {
        _configs = new HashMap<String, int[]>();
        _configs.put("ardri", ARD_RI);
        _configs.put("tablut", TABLUT);
        _configs.put("errk", ERRK);
    }
    
}
