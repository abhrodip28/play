package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {
	
	
	/*
	public static HashMap<String, String> awesomeMethod(){
	//public static Map<String, String> awesomeMethod(){
		return null;
	}
	*/

    public static void index() {
		//awesomeMethod();
		new stuff.Stuff().awesomeMethod();
        render();
    }

}