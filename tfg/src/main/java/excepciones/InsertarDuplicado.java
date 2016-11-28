package excepciones;

public class InsertarDuplicado extends Exception{

	public InsertarDuplicado(){
		super("El objeto que intenta insertar ya existe en la BBDD");
	}
}
