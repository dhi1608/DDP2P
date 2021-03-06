package net.ddp2p.common.data;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
public class D_Interests_Message extends ASNObj{
	public byte[] interests = new byte[0];
	public byte[] message = new byte[0];
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(interests));
		enc.addToSequence(new Encoder(message));
		return enc;
	}
	@Override
	public D_Interests_Message decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		interests = d.getFirstObject(true).getBytes();
		message = d.getFirstObject(true).getBytes();
		return this;
	}
}
