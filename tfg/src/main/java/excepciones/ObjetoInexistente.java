package excepciones;

public class ObjetoInexistente extends Exception{

	public ObjetoInexistente(){
		super("El objeto que intenta borrar no ha sido guardado o cargado");
	}
}
