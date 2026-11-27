/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package resources;

public class GeneralResourcesPaths {

	public static String WORDLIST_C;
	public static String WORDLIST_Csharp;
	public static String WORDLIST_Cpp;
	public static String WORDLIST_JAVA;
	public static String WORDLIST_HTML;
	public static String WORDLIST_CSS;
	public static String WORDLIST_JS;
	public static String WORDLIST_HASKELL;
	public static String WORDLIST_PHP;
	public static String WORDLIST_PYTHON;
	public static String WORDLIST_RUBY;
	public static String WORDLIST_SQL;
	public static String WORDLIST_BASH;
	
	public static void init() {
		WORDLIST_C = GeneralConfig.WORDLIST_FOLDER + "Wordlist_C.md";            
		WORDLIST_Csharp = GeneralConfig.WORDLIST_FOLDER + "Wordlist_C#.md";      
		WORDLIST_Cpp = GeneralConfig.WORDLIST_FOLDER + "Wordlist_C++.md";        
		WORDLIST_JAVA = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Java.md";      
		WORDLIST_HTML = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Html.md";      
		WORDLIST_CSS = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Css.md";        
		WORDLIST_JS = GeneralConfig.WORDLIST_FOLDER + "Wordlist_JavaScript.md";  
		WORDLIST_HASKELL = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Haskell.md";
		WORDLIST_PHP = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Php.md";        
		WORDLIST_PYTHON = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Python.md";  
		WORDLIST_RUBY = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Ruby.md";      
		WORDLIST_SQL = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Sql.md";        
		WORDLIST_BASH = GeneralConfig.WORDLIST_FOLDER + "Wordlist_Bash.md";      
	}
}
