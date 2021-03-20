package validation.fwk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.TestUtils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import utils.TestUtils;

public class InputValidationTestHelper {
	
	@Rule public TestName currentTestName = new TestName();
	
	private final static ValidatorFactory defaultValidatorFactory = Validation.buildDefaultValidatorFactory();
	
    private Validator validator;
    private ObjectMapper mapper;
    private InputJsonTestCases testCaseSource;
    private Class<?> requestClass;
    private Object requestInstance;
	private boolean testPassed = true;

	public ArrayList<String> esitList = new ArrayList<String>();
	
    /**
     * Crea un oggetto helper, sul quale è possibile invocare i metodi di test positivo e negativo, passando la classe
     * che contiene gli array di stringhe json: i casi di test OK/KO. Per far ciò basta creare una classe che implementa l'interfaccia
     * <b>InputJsonTestCases</b>.
     * 
     * Quindi, per testare il parsing e la validazione con semplicità, basta instanziare un oggetto InputValidationTestHelper e chiamarne i metodi di test [Setup,OKTest,KOTest] :)
     * 
     * @param testCaseSource -> la classe che implementa InputJsonTestCases e fornisce i json di test in formato stringa
     * @param clazz -> la classe della request contenente le annotazione di validazione <i>javax.validation</i>
     * 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public <E extends InputJsonTestCases,T> InputValidationTestHelper(Class<E> testCaseSourceClass, Class<T> clazz) throws InstantiationException, IllegalAccessException {
    	this.testCaseSource = testCaseSourceClass.newInstance();
    	requestClass = clazz;
        mapper = new ObjectMapper();
        validator = null;
    }

    /**
     * Crea un oggetto helper, sul quale è possibile invocare i metodi di test positivo e negativo, passando la classe
     * che contiene gli array di stringhe json: i casi di test OK/KO. Per far ciò basta creare una classe che implementa l'interfaccia
     * <b>InputJsonTestCases</b>.
     * 
     * Quindi, per testare il parsing e la validazione con semplicità, basta instanziare un oggetto InputValidationTestHelper e chiamarne i metodi di test [Setup,OKTest,KOTest] :)
     * 
     * @param testCaseSource -> la classe che implementa InputJsonTestCases e fornisce i json di test in formato stringa
     * @param clazz -> la classe della request contenente le annotazione di validazione <i>javax.validation</i>
     * 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public <E extends InputJsonTestCases,T> InputValidationTestHelper(Validator validatorCustom, Class<E> testCaseSourceClass, Class<T> clazz) throws InstantiationException, IllegalAccessException {
    	this.testCaseSource = testCaseSourceClass.newInstance();
    	requestClass = clazz;
    	validator = validatorCustom;
        mapper = new ObjectMapper();
    }

    /**
     * Crea un oggetto helper, sul quale è possibile invocare i metodi di test positivo e negativo, passando la classe
     * che contiene gli array di stringhe json: i casi di test OK/KO. Per far ciò basta creare una classe che implementa l'interfaccia
     * <b>InputJsonTestCases</b>.
     * 
     * Quindi, per testare il parsing e la validazione con semplicità, basta instanziare un oggetto InputValidationTestHelper e chiamarne i metodi di test [Setup,OKTest,KOTest] :)
     * 
     * @param testCaseSource -> la classe che implementa InputJsonTestCases e fornisce i json di test in formato stringa
     * @param clazz -> la classe della request contenente le annotazione di validazione <i>javax.validation</i>
     * 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public <E extends InputJsonTestCases,T> InputValidationTestHelper(ObjectMapper mapperCustom, Validator validatorCustom, Class<E> testCaseSourceClass, Class<T> clazz) throws InstantiationException, IllegalAccessException {
    	this.testCaseSource = testCaseSourceClass.newInstance();
    	requestClass = clazz;
    	validator = validatorCustom;
    	mapper = mapperCustom;
    }
    
    /**
     * Richiamare questo metodo nel @Before.
     * E' possibile utilizzare [ @Rule public TestName currentTestName = new TestName() ]
     * e passare il suo valore come parametro tramite la chiamata <i>currentTestName.getMethodName()</i>.
     * 
     */
    public void setup(String currentTestMethod) {
    	
    	printHeader("#", 40, "PREPARING TEST: " + currentTestMethod);
    	
    	validator = (validator == null)? defaultValidatorFactory.getValidator() : validator;
        testPassed = true;
        
        printl("\n   => Validator initialized. Starting test ["+currentTestMethod+"]");
    }

    /**
     * Richiama questo metodo nel Test positivo... e dovrebbe far tutto lui :)
     */
	public void inputValidationOkTest() throws IOException {
		
		if(mapper == null || validator == null) {
			setup(currentTestName.getMethodName());
		}
		
		printHeader("#", 40, "EXECUTING [OK] USE_CASE BATCH : VALIDATION MUST PASS... ");
		
		// leggo dall'interfaccia testCaseSource 
		// (la classe che la implementa definisce i casi OK/KO con due appositi metodi)
		ArrayList<String> koTestCasesJson = testCaseSource.getJsonCasesOK();
		int i = 1;
		
		try {
			for (String jsonSource : koTestCasesJson) {
				// Per ogni caso di test OK parso la request in un oggetto del tipo della request sotto test:
				// Qui avviene la mappatura tra json stringa e oggetto della request da testare
				requestInstance = mapper.readValue(jsonSource, requestClass);
				Set<ConstraintViolation<Object>> violations = validator.validate(requestInstance);
				if (!violations.isEmpty()) {
					testPassed = false;
					printl("\n\n=> VALIDAZIONE TEST_JSON_SIMULAZIONE_OK (" + i
						+ ") FALLITA\n\n  => ! ATTENZIONE ! (Qualcosa non è andato come previsto, verificare: in questo test la validazione dovrebbe essere OK) \n\n => VIOLATION LIST: ");
				} else {
					printl("\n\n=> VALIDAZIONE TEST_JSON_SIMULAZIONE_OK (" + i + ") SUPERATA -> VIOLATION LIST: ");
				}
				
				printArrayAsLines(1, violations.toArray());
				String jsonPrettified = getPrettyStringJsonObj(requestInstance);
				printl("\n\n=> JSON SOURCE(" + i + "): " + jsonPrettified + "\n");
				
				assertTrue(violations.isEmpty());
				++i;
			} 
		} catch (Throwable e) {
			TestUtils.printExceptionStackString(e);
			throw e;
		} finally {
			String nameofCurrMethod = new Exception().getStackTrace()[0].getMethodName();
			updateTestEsit(testPassed, nameofCurrMethod);
		}
		
	}
	
    /**
     * Richiama questo metodo nel Test negativo... e dovrebbe far tutto lui :)
     */
	public void inputValidationKoTest() throws IOException {
		
		if(mapper == null || validator == null) {
			setup(currentTestName.getMethodName());
		}
		
		printHeader("#", 40, "EXECUTING [KO] USE_CASE BATCH : VALIDATION MUST CATCH VIOLATIONS... ");

		// leggo dall'interfaccia testCaseSource 
		// (la classe che la implementa definisce i casi OK/KO con due appositi metodi)		
		ArrayList<String> koTestCasesJson = testCaseSource.getJsonCasesKO();
		int i = 1;
		
		try {
			for (String jsonSource : koTestCasesJson) {
				// Per ogni caso di test KO parso la request in un oggetto del tipo della request sotto test:
				// Qui avviene la mappatura tra json stringa e oggetto della request da testare
				requestInstance = mapper.readValue(jsonSource, requestClass);
				Set<ConstraintViolation<Object>> violations = validator.validate(requestInstance);
				if (!violations.isEmpty()) {
					printl("\n\n=> TEST_JSON_SIMULAZIONE_KO (" + i + ")  : FORMATO ERRATO RILEVATO DURANTE LA VALIDAZIONE -> VIOLATION LIST: ");
				} else {
					testPassed = false;
					printl("\n\n=> VALIDAZIONE TEST_JSON_SIMULAZIONE_KO SUPERATA (" + i
						+ ")  ! ATTENZIONE ! (Qualcosa non è andato come previsto, verificare: in questo test dovrebbero essere rilevati errori di validazione!)");
				}

				printArrayAsLines(1, violations.toArray());
				String jsonPrettified = getPrettyStringJsonObj(requestInstance);
				printl("\n\n=> JSON SOURCE(" + i + "): " + jsonPrettified + "\n");
				
				assertFalse(violations.isEmpty());
				++i;
			} 
		} catch (Throwable e) {
			TestUtils.printExceptionStackString(e);
			throw e;
		} finally {
			String nameofCurrMethod = new Exception().getStackTrace()[0].getMethodName();
			updateTestEsit(testPassed, nameofCurrMethod);
		}
		
	}
	
	private void updateTestEsit(boolean esit, String testName) {
		if(!esit) {
			esitList.add(TestUtils.getHeaderString("*", 40, testName + " -> TEST FALLITO."));
		}else {
			esitList.add(TestUtils.getHeaderString("@", 40, testName + " -> TEST SUPERATO."));
		}
	}
	
	/**
	 * Richiamare questo metodo per stampare i risultati del test (utilizzare @AfterClass a questo scopo)
	 */
	public void printTestEsit() {
		TestUtils.printHeader("?", 80, " ESITI DEL TEST ");
		TestUtils.printArrayAsLines(1, esitList.toArray());
	}
	
	/**
	 * Retrieve a POJO object instance as Json string, with indentation
	 */
	public static String getPrettyStringJsonObj(Object source) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(source);
	
		return prettyJson;
	}
	
	public static Object readGenericJsonString(String input) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		Object json = mapper.readValue(input, Object.class);
		
		return json;
	}
	
	public static String prettyFormatJsString(String input) throws JsonParseException, JsonMappingException, IOException {
		
		Object json = readGenericJsonString(input);
		
		return getPrettyStringJsonObj(json);
	}
}
