/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package data;

import util.Summary;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class ASNConstituentOP extends ASNObj implements Summary{
	//public ASNConstituent constituent;
	public D_Constituent constituent;
	public int op=0; // DEFAULT add(0) [remove(1), change(2)]
	public String toString() {
		return "ASNConstituentOP: "+
		"; op="+op+
		"; constituent="+constituent+
		"";
	}
	public String toSummaryString() {
		return constituent.toSummaryString();
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(constituent.getEncoder());
		if(op!=0)enc.addToSequence(new Encoder(op));
		return enc;
	}

	@Override
	public Object decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		//constituent = new ASNConstituent().decode(dec.getFirstObject(true));
		constituent = new D_Constituent().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()!=0) op = dec.getFirstObject(true).getInteger().intValue();
		if(dec.getTypeByte()!=0) throw new ASN1DecoderFail("Redundant content in ASNConstituentOp");
		return this;
	}
	public ASNConstituentOP instance(){
		return new ASNConstituentOP();
	}
}
