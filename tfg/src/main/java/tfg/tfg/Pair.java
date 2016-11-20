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

}
