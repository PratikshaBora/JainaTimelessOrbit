package com.timelessOrbit.gamestate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards")
public class Card {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@Enumerated(EnumType.STRING)
	public Aara aara;
	@Enumerated(EnumType.STRING)
    public Dwar dwar;
	@Enumerated(EnumType.STRING)
    public CardType type;
    public String imageURL; // ✅ new field
    public int pointValue;
    @Enumerated(EnumType.STRING)
    public Aara newAara;
    
    public Card() {
		super();
		// TODO Auto-generated constructor stub
	}
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
