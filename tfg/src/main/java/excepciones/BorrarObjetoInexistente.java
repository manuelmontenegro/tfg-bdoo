package excepciones;

public class BorrarObjetoInexistente extends Exception{

	public BorrarObjetoInexistente(){
		super("El objeto que intenta borrar no ha sido guardado o cargado");
	}
}
