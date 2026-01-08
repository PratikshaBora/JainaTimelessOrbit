package com.timelessOrbit.gamestate;

public class Card {
	
	public Aara aara;
    public Dwar dwar;
    public CardType type;
    public String imageURL; // ✅ new field
    public int pointValue;
    
    public Card(Aara aara, Dwar dwar) {
        this.aara = aara;
        this.dwar = dwar;

        if (dwar == Dwar.SKIP || dwar == Dwar.REVERSE || dwar == Dwar.ADD2)
            this.type = CardType.ACTION;
        else if (dwar == Dwar.COLOR_CHANGE || dwar == Dwar.COLOR_CHANGE_ADD4)
            this.type = CardType.WILD;
        else
            this.type = CardType.NORMAL;
        
     // ✅ Build image URL based on naming convention
        this.imageURL = "/assets/cards/" 
                        + aara.name().toLowerCase() 
                        + "_" 
                        + dwar.name().toLowerCase() 
                        + ".jpg";
        
        setPointValue();
    }
    public int getPointValue() {
    	return pointValue;
    }
    public Aara getAara() {
		return aara;
	}
	public void setAara(Aara aara) {
		this.aara = aara;
	}
	public Dwar getDwar() {
		return dwar;
	}
	public void setDwar(Dwar dwar) {
		this.dwar = dwar;
	}
	public CardType getType() {
		return type;
	}
	public void setType(CardType type) {
		this.type = type;
	}
	public String getImageURL() {
		return imageURL;
	}
	public void setPointValue() {
		switch (dwar) {
        	case SKIP:
        	case REVERSE:
        	case ADD2:
        		pointValue = 5;
        		break;
        	case COLOR_CHANGE:
        	case COLOR_CHANGE_ADD4:
        		pointValue = 10;
        		break;
        	default:
        		pointValue = 2; // normal cards
            break;
		}
	}
	@Override
    public String toString() {
        return aara + "_" + dwar;
    }
}
