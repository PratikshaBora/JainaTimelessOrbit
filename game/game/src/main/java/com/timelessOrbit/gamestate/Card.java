package com.timelessOrbit.gamestate;

public class Card {
	/*
	 * String aara; String dwar;
	 * 
	 * 	public Card(String aara, String dwar) { 
	 * 	super(); 
	 * 	this.aara = aara; 
	 * 	this.dwar = dwar; 
	 * }
	 */
	
	public Aara aara;
    public Dwar dwar;
    public CardType type;

    public Card(Aara aara, Dwar dwar) {
        this.aara = aara;
        this.dwar = dwar;

        if (dwar == Dwar.SKIP || dwar == Dwar.REVERSE || dwar == Dwar.ADD2)
            this.type = CardType.ACTION;
        else if (dwar == Dwar.COLOR_CHANGE || dwar == Dwar.COLOR_CHANGE_ADD4)
            this.type = CardType.WILD;
        else
            this.type = CardType.NORMAL;
    }
    
    public int getPointValue() {
        switch (dwar) {
            case SKIP:
            case REVERSE:
            case ADD2:
                return 5;

            case COLOR_CHANGE:
            case COLOR_CHANGE_ADD4:
                return 10;

            default:
                return 1; // normal cards
        }
    }

    @Override
    public String toString() {
        return aara + " - " + dwar;
    }
}
