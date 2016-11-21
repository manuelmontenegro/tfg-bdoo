package tfg.tfg;

/**
 * Clase para representar una pareja de dos atributos
 * @author Carlos
 *
 */
public class Pair <A,B>{
	
	private A objectA;
	private B objectB;
	
	public Pair(A a, B b){
		this.objectA = a;
		this.objectB = b;
	}
	
	public A getObjectA() {
		return objectA;
	}
	public void setObjectA(A objectA) {
		this.objectA = objectA;
	}
	public B getObjectB() {
		return objectB;
	}
	public void setObjectB(B objectB) {
		this.objectB = objectB;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			Pair tmpPersona = (Pair) obj;
			if (this.objectA.equals(tmpPersona.objectA) && this.objectB.equals(tmpPersona.objectB)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

}
