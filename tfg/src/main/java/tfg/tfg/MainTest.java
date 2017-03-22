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
		
		for (Field f : mt.getClass().getDeclaredFields()) {
			f.setAccessible(true); 
	

			Type friendsGenericType = f.getGenericType();
			ParameterizedType friendsParameterizedType = (ParameterizedType) friendsGenericType;
			Type[] friendsType = friendsParameterizedType.getActualTypeArguments();
			Class userClass = (Class) friendsType[0];
			Object o = null;
			try {
				o = userClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(o.getClass().getName());
			System.out.println(f.getName());
		}
		
	}

}
