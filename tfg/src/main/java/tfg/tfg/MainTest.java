package tfg.tfg;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import profundidad3.Usuario;

public class MainTest {

	private List<Usuario> lista;
	
	public MainTest(){
		this.lista = new ArrayList<Usuario>();
	}

	public static void main(String[] args) {
		MainTest mt = new MainTest();
		
		List<String> prueba = new ArrayList<String>();
		System.out.println(prueba.getClass().getName());

		
		
	}

}
