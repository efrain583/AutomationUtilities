package Util;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
//import com.jcabi.log.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fluttercode.datafactory.impl.DataFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.gargoylesoftware.htmlunit.BrowserVersion;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;

import org.testng.ITestResult;
import org.testng.annotations.Test;

public class UtilKit {

	private static String projectFolder = System.getenv("GIT_LOCAL_REPOSITORY");

	private static String resourcesFolder = "/src/test/resources";
	private static String projectName = null;
	private static String application;
	private static String className;
	private static Properties configProperties = new Properties();
	private static Properties UIMapProperties = new Properties();
	private static Properties DBProperties = new Properties();
	private static String logLevel = null;
	public static Logger logger = null;
	public static StopWatch stopWatch = new StopWatch();
	public static DataFactory dataFactory = new DataFactory();
	private static long startTime;
	private static long endTime;
	private static String browser;
	private static String version;

	private static Connection dbConn = null; // JDBC Connection

	public static WebDriver initTest(String project, String inApplication, String inBrowser, String inClassName) {

		WebDriver driver = null;
		
		projectName = project;
		className = inClassName;
		application = inApplication;
		DesiredCapabilities caps = null;

		// Load configurations
		UtilKit.loadConfigProperties(application);
		UtilKit.loadUIMapProperties(application);

		if (logger == null) {

			UtilKit.createLogger();
		}	

		logger.info( "============================================================================================" );
		if(projectFolder.isEmpty())
			projectFolder = System.getenv("GIT_LOCAL_REPOSITORY");
		logger.info( "Project : " + project + "          Application : " + inApplication );
		logger.info("Project Repository: " + projectFolder);
		logger.info("============================================================================================" );
		if(inBrowser.isEmpty())
			browser = getConfigProp("BROWSER");
		else
			browser=inBrowser;
		if (browser.equalsIgnoreCase("firefox")){
			System.setProperty("webdriver.gecko.driver", getConfigProp("GECKO_DRIVER"));
			logger.info("Gecko Driver prop : " + System.getProperty("webdriver.gecko.driver"));
			caps  = DesiredCapabilities.firefox();
			caps.setCapability("browserName", browser);
			//caps.setCapability("logLevel", "DEBUG");
			//caps.setCapability("logLevel", "SEVERE");
			caps.setCapability("logLevel", "OFF");
			caps.setCapability("requireWindowFocus", true);
// Skip the firefox version for now
			version = getConfigProp("BROWSER_VERSION");
//			caps.setCapability("version", version);
//			caps.setVersion(version);
			driver = new FirefoxDriver(caps);
			driver.manage().deleteAllCookies();
			// The Implicit wait time is a property and apply for all findElement() calls
			driver.manage().timeouts().implicitlyWait(Long.valueOf(getConfigProp("IMPLICIT_WAIT")), TimeUnit.SECONDS);
			driver.manage().timeouts().pageLoadTimeout(Long.valueOf(getConfigProp("PAGE_LOAD_WAIT")), TimeUnit.SECONDS);
			// navegate to application Url
			navegateToBaseURL(driver);
		}
		else if (browser.equalsIgnoreCase("ie")) {
			
			/*
			 * To make it work with IE The Windows SEcurity Update KB3024390 was un-installed.
			 */
			System.setProperty("webdriver.ie.driver", getConfigProp("IE_DRIVER")); // Set IE driver Path
			caps = DesiredCapabilities.internetExplorer();
			caps.setCapability("ignoreZoomSetting", true);
			caps.setCapability("requireWindowFocus", true);
			caps.setCapability("enablePersistentHover", true);
			caps.setCapability("disable-popup-blocking", true);
			caps.setCapability("ignoreProtectedModeSettings", true);
			caps.setCapability("nativeEvents", false);
			caps.setCapability("unexpectedAlertBehaviour", "accept");
			
			//caps.setCapability("logLevel", "DEBUG");
			driver = new InternetExplorerDriver(caps);
			// The Implicit wait time is a property and apply for all findElement() calls
			driver.manage().timeouts().implicitlyWait(Long.valueOf(getConfigProp("IMPLICIT_WAIT")), TimeUnit.SECONDS);
			driver.manage().timeouts().pageLoadTimeout(Long.valueOf(getConfigProp("PAGE_LOAD_WAIT")), TimeUnit.SECONDS);
			// IE hides cookies until the URL is accessed so
			// We need to navegate to application Url first.
			// then clear cookies, then Navegate again 
			navegateToBaseURL(driver);
			driver.manage().deleteAllCookies();
			// navegate Again to application Url
			navegateToBaseURL(driver);
		} else {
			logger.fatal("Initialization FATAL Error : Invalid Browser : " + browser + " Exiting Test .........");
			System.exit(10);

		}

		// The Implicit wait time is a property and apply for all findElement() calls
		driver.manage().timeouts().implicitlyWait(Long.valueOf(getConfigProp("IMPLICIT_WAIT")), TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(Long.valueOf(getConfigProp("PAGE_LOAD_WAIT")), TimeUnit.SECONDS);
		
		UtilKit.waitForPageToLoad(driver, 5);
		logger.info("Browser : " + browser + " Version : " + caps.getCapability("version"));
		logger.info(("\n\t\tImplicit wait = " + getConfigProp("IMPLICIT_WAIT") + "\n"));
		logger.info("Driver Time out String " + driver.manage().timeouts().toString());

		//driver.manage().window().maximize();
		
		if (logger == null) {
			UtilKit.createLogger();
		}	

		return driver;
	}

	/**
	 * 
	 */
	private static void createLogger() {
		System.setProperty("com.jcabi.log.coloring", "true"); // Not working in windows for now
		logger = Logger.getLogger(application + "." + className);
		PropertyConfigurator.configure(projectFolder + "/" + projectName  + "/" + application + resourcesFolder + "/log4j.properties");

		NDC.pop(); // Make sure the stack is empty first
		NDC.push(application.toUpperCase());

		logLevel = UtilKit.getConfigProp("LOG_LEVEL");
		if(logLevel != null){
			logger.setLevel(Level.toLevel(logLevel));
		}
		logger.info("LOG LEVEL : " + logger.getEffectiveLevel().toString());
	}

	public static void initMethod(Method  method) {

		if (logger == null) {
			UtilKit.createLogger();
		}	

		startTime = System.currentTimeMillis();
		if(UtilKit.getConfigProp("PERFORMANCE").equalsIgnoreCase("yes") || methodInGroup(method, "performance")){
			if(stopWatch.isStarted())
				stopWatch.stop(); // Make sure it starts fresh
			stopWatch.reset();
			stopWatch.start();
		}
		logger.info("Test Method : " + className + ":" + method.getName() + " Started at " + getDateTime());
	}

	/**
	 * @param method
	 */
	public static boolean methodInGroup(Method method, String inGroup) {
		Test testAnnotation = method.getAnnotation(Test.class);
		String[] testGroups = testAnnotation.groups();
		for(String testGroup :  testGroups){
			if(testGroup.equalsIgnoreCase(inGroup)){
				logger.info(method.getName() + " In Group : " + testGroup);
				return true;
			}
		}
		return false;
	}

	private static boolean dbConnect() {

		logger.debug("Entering dbConnect");
		logger.info("Connecting to " + getDBProp("DBCONN_URL"));
		try {
			// Initiate a JDBC Connection
			dbConn = DriverManager.getConnection(getDBProp("DBCONN_URL"), getDBProp("DBUSER"), getDBProp("DBPASS"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.fatal("DB Connection Exception : " + getDBProp("DBCONN_URL"));
			e.printStackTrace();
			System.exit(10);
		}
		try {
			if (dbConn.isValid(10)) { // 10 seconds timeout
				logger.info("DB Connection Success : " + getDBProp("DBCONN_URL"));
				logger.debug("Exiting dbConnect");
				return true;
			}
		} catch (SQLException e) {
			logger.fatal("DB Connection Invalid Exception : " + getDBProp("DBCONN_URL"));
			e.printStackTrace();
			System.exit(10);
		}
		logger.debug("Exiting dbConnect");
		return false;
	}

	public static HtmlUnitDriver initUnitTest(String application, String testMethodName) {

		HtmlUnitDriver driver = null;

		UtilKit.loadConfigProperties(application);
		UtilKit.loadUIMapProperties(application);

		browser = getConfigProp("BROWSER");
		if (browser.equals("firefox")) {
			driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_52);

		} else if (browser.equalsIgnoreCase("explorer")) {
			driver = new HtmlUnitDriver(BrowserVersion.INTERNET_EXPLORER);
		} else {
			logger.fatal("Invalid Browser : " + browser + " Exiting Test .........");
			System.exit(10);

		}

		if (logger == null) {
			UtilKit.createLogger();
		}

		// driver.setJavascriptEnabled(true); // Only for headles, enable
		// javascript
		driver.setJavascriptEnabled(false); // Only for headles enable
											// javascript

		driver.manage().timeouts().implicitlyWait(Long.valueOf(getConfigProp("IMPLICIT_WAIT")), TimeUnit.SECONDS);

		navegateToBaseURL(driver);
		driver.manage().deleteAllCookies(); // here so that it works in IE

		logger.info("Headless Mode, Domain Name: " + (String) driver.executeScript("return document.domain")); // Needs
																												// javascript
																												// enabled

		logger.info("Test Method : " + testMethodName + " Started at " + getDateTime());
		startTime = System.currentTimeMillis();
		return driver;
	}

	public static void navegateToBaseURL(WebDriver driver) {
		
		//Get the Application URl
		driver.get(getConfigProp(application.toUpperCase() + "_URL"));

	}

	// ITestResult is a Testng Object This class describes the result of a test.
	public static void terminateMethod(WebDriver driver, ITestResult result) {

		endTime = System.currentTimeMillis();

		/*
		 * If there is a need to take a look at the results then un-comment this
		 * block Set<String> atNames = result.getAttributeNames(); for (String
		 * namestr : atNames) { System.out.println("Result Attribute : " +
		 * namestr); }
		 */

		// Print current screen if the test case was not successful
		if (!result.isSuccess()) {
			UtilKit.printScreen(driver, result.getMethod().getMethodName());
			logger.error("Test Method : " + className + "."+ result.getMethod().getMethodName() + " Completed at : " + getDateTime()
				+ " Elapsed Time in Seconds : " + (float) (endTime - startTime) / 1000 + "  Status = FAIL"  + "\n");

		}
		else {
			logger.info("Test Method : " + className + "."+ result.getMethod().getMethodName() + " Completed at : " + getDateTime()
				+ " Elapsed Time in Seconds : " + (float) (endTime - startTime) / 1000 + " Status = SUCCESS"  +"\n");
			
		}
		if(getConfigProp("PERFORMANCE").equalsIgnoreCase("true") || methodInGroup(result, "performance")){
			float elapsedTime = ((float)stopWatch.getTime()) /1000;
			logger.info(result.getMethod().getMethodName() + "  PERFORMANCE Time: " + elapsedTime);
			stopWatch.stop();
		}
		UtilKit.suspendAction(1000);
	}

	/**
	 * @param result
	 */
	public static boolean methodInGroup(ITestResult result, String inGroup) {
		String[] methodGroups = result.getMethod().getGroups();
		for (String methodGroup : methodGroups) {
			if (methodGroup.equalsIgnoreCase(inGroup)) {
				logger.info(result.getMethod().getMethodName() + " In Group :" + methodGroup);
				return true;
			}
		}
			return false;
	}

	public static void terminateTest(WebDriver driver) {
		driver.manage().deleteAllCookies();
		UtilKit.suspendAction(1000);
	//	driver.close(); // Close Windows (redundant since quit() also closes all browser windows)
		driver.quit(); // End Session Safely
	}

	// Take the Screen Shot and capture in a file
	static public void printScreen(WebDriver driver, String methodName) {
		try {
			String destFileName = projectFolder + "/" + projectName + "/" + application + "/screenPrints/" + methodName + "_" + getPlainDateTime() + ".png";
			File destFile = new File(destFileName);

			logger.info("Screen Print File Name : " + destFileName);
			destFile.createNewFile();

			/*
			 * Since Driver Classes like FirefoxDriver and
			 * InternetExplorerDriver implement WebDriver and TakesScreenshot
			 * interfaces, then we can cast the driver object as a
			 * TakesScreenshot object to be able to invoke getScreenshotiAs().
			 */
			File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

			// Copy the screen shot the the destination file
			FileUtils.copyFile(srcFile, destFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void terminateTestUnit(HtmlUnitDriver driver, ITestResult result) {

		endTime = System.currentTimeMillis();

		logger.info("Test Method : " + result.getMethod().getMethodName() + " Completed at : " + getDateTime()
				+ " Elapsed Time in Seconds : " + (float) (endTime - startTime) / 1000 + "\n\n");

		// Print current screen if the test case was not successful
		if (!result.isSuccess()) {
			UtilKit.printScreen(driver, result.getMethod().getMethodName());

		}

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		driver.close();
		driver.quit();
	}

	protected static void loadConfigProperties(String application) {

		FileInputStream configPropFile = null;
		try {
			// The load() call requires an InputStream Object.
			// FileInputStream extends InputStream so it can also be used here
			// as a parameter to load()
			configPropFile = new FileInputStream(projectFolder + "/" + projectName + "/" +  application  + resourcesFolder + "/MasterTestConfig.properties");
			;
			configProperties.load(configPropFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static void loadUIMapProperties(String Application) {

		FileInputStream UIMapInputFile = null;
		try {
			// The load() call requires an InputStream Object.
			// FileInputStream extends InputStream so it can also be used here
			// as a parameter to load()
			UIMapInputFile = new FileInputStream(getConfigProp(Application.toUpperCase() + "_UIMAP"));
			UIMapProperties.load(UIMapInputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static void loadDBProperties(String Application) {

		// The load() call requires an InputStream Object.
		// FileInputStream extends InputStream so it can also be used here as a
		// parameter to load()
		try {
			FileInputStream DBInputFile = new FileInputStream(projectFolder + "/" + projectName + "/" + application + resourcesFolder + "/DBTest.properties");
			DBProperties.load(DBInputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getConfigProp(String property) {
		if(configProperties.getProperty(property) == null){
			logger.warn("Property : " + property + " Undefined ") ;
			return String.valueOf("UNDEFINED");
		}
		return (configProperties.getProperty(property));
	}

	public static String getUIMapProp(String property) {
		return (UIMapProperties.getProperty(property));
	}

	public static String getDBProp(String property) {
		return (DBProperties.getProperty(property));
	}

	/*
	 * The By locator info is stored as property in the UIMAP file The Target
	 * WebElement name itself corresponds to Property Name in the UIMAP file
	 * 
	 * The UIMAP file format is: PROPERTY = LOCATOR_TYPE.LOCATOR_VALUE ex.
	 * LOGIN_BUTTOM = xpath..//*[@id='login']
	 */
	public static By UIMap(String property) {

		String[] propStrings = parseLocator(property);

		return returnBy(propStrings);
	}

	public static By returnBy(String[] propStrings) {
		// Return the corresponding By Object.
		// This is done by calling the matching By method like By.id(),
		// By.xpath(), By.name(), etc
		// Each of those methods returns a By object which locates elements
		// by the given type and value attributes
		if (propStrings[0].equals("id")) {
			return (By.id(propStrings[1]));
		}
		if (propStrings[0].equals("xpath")) {
			return (By.xpath(propStrings[1]));
		}
		if (propStrings[0].equals("css")) {
			return (By.cssSelector(propStrings[1]));
		}
		if (propStrings[0].equals("name")) {
			return (By.name(propStrings[1]));
		}
		if (propStrings[0].equals("partialLinkText")) {
			return (By.partialLinkText(propStrings[1]));
		}
		if (propStrings[0].equals("tagName")) {
			return (By.tagName(propStrings[1]));
		}

		logger.fatal("Invalid Locator :" + propStrings[0] + "." + propStrings[1]);
		return null;
	}

	public static String[] parseLocator(String property) {
		// Parse out the the locator type and locator value,
		// type is stored in propStrings[0] and value in propStrings[1]
		String[] propStrings = UtilKit.getUIMapProp(property).split(Pattern.quote("."), 2);
		return propStrings;
	}

	public static boolean waitForElement(By Locator, WebDriver driver, String state, int waitPeriod) {

		float currentWait = (float) 0.0;
		logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + "Locator String :" +     Locator.toString());
		while (currentWait < (float) waitPeriod) {

			// =================================================================
			if (state.equalsIgnoreCase("Exists")) {
				List<WebElement> waitElementList = driver.findElements(Locator);
				if (waitElementList.size() > 0) {
					logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + " Waited for "
							+ currentWait + " Seconds...");
					return true;
				}
			}
			// =================================================================
			if (state.equalsIgnoreCase("Displayed")) {
				List<WebElement> waitElementList = driver.findElements(Locator);
				if (waitElementList.size() > 0) {
					WebElement waitElement = driver.findElement(Locator);
					if (waitElement.isDisplayed()) {
						logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + " Waited for "
								+ currentWait + " Seconds...");
						return true;
					}
				}
			}
			// =================================================================
			if (state.equalsIgnoreCase("Enabled")) {
				List<WebElement> waitElementList = driver.findElements(Locator);
				if (waitElementList.size() > 0) {
					WebElement waitElement = driver.findElement(Locator);
					if (waitElement.isEnabled()) {
						logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + " Waited for "
								+ currentWait + " Seconds...");
						return true;
					}
				}
			}
			// ===============================================================
			if (state.equalsIgnoreCase("Selected")) {
				List<WebElement> waitElementList = driver.findElements(Locator);
				if (waitElementList.size() > 0) {
					WebElement waitElement = driver.findElement(Locator);
					if (waitElement.isSelected()) {
						logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + " Waited for "
								+ currentWait + " Seconds...");
						return true;
					}
				}
			}
			/*
			 * Do the actual wait here by sleeping for .25 of a second
			 */
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			currentWait = (float) (currentWait + (1.0 / 4.0)); // Add the
																// accumulated
																// time to
																// currentWait

		}
		// If it made it here then the time was expired and the Element did not
		// reach the desired status
		logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + ": " + state + ": " + Locator.toString() + " Expired after "
				+ currentWait + " Seconds...");
		return false;

	}
	
	public static boolean waitForElement(WebElement waitElement, String state, int waitPeriod) {

		float currentWait = (float) 0.0;
		while (currentWait < (float) waitPeriod) {
			
			logger.info("Curren Wait : " + currentWait);

			// =================================================================
			try {
				if (state.equalsIgnoreCase("Displayed")) {
						if (waitElement.isDisplayed()) {
							logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + 
									" on Displayed state " +
									" Waited for " + currentWait + " Seconds...");
							return true;
						}
					}
				// =================================================================
				if (state.equalsIgnoreCase("Enabled")) {
						if (waitElement.isEnabled()) {
							logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + 
									" on Enabled state " +
									" Waited for " + currentWait + " Seconds...");
							return true;
						}
					}
				// ===============================================================
				if (state.equalsIgnoreCase("Selected")) {
						if (waitElement.isSelected()) {
							logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + 
									" on Selected State " +
									" Waited for " + currentWait + " Seconds...");
							return true;
						}
					}
			} catch (Exception StaleElementReferenceException) {
				logger.warn("Stale exception - 1");
			}
			/*
			 * Do the actual wait here by sleeping for .25 of a second
			 */
			try {
				Thread.sleep(250);
				logger.warn("Stale exception - 2");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			currentWait = (float) (currentWait + (1.0 / 4.0)); // Add the
																// accumulated
																// time to
																// currentWait

		}
		// If it made it here then the time was expired and the Element did not
		// reach the desired status
		logger.warn("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + ": " + state + " TagName: " + waitElement.getTagName() + " Expired after "
				+ currentWait + " Seconds...");
		return false;

	}


	public static boolean waitForPageToLoad(WebDriver driver, int waitPeriod) {

		float currentWait = (float) 0.0;
		logger.info("In " + Thread.currentThread().getStackTrace()[1].getMethodName());
		while (currentWait < (float)waitPeriod ) {
			
			String pageState = null;

			// =================================================================
			try{
				pageState = (String) UtilKit.executeJavascript(driver, "return document.readyState");
			}
			catch (JavascriptException e){
				
			UtilKit.suspendAction(250);;
			currentWait = currentWait + (float)0.25;
			continue;
			}
			logger.debug("Page State : " + pageState);
			if(pageState.equals("complete")){
				logger.debug("Current Page Title : " + driver.getTitle());
				return true;

			}
		}
		
		logger.warn("Wait for page to load expired");
		return false;
	}	

	public static boolean waitForPageTitle(WebDriver driver, int waitPeriod, String title) {

		float currentWait = (float) 0.0;
		logger.info("in : " + Thread.currentThread().getStackTrace()[1].getMethodName());
		while (currentWait < (float)waitPeriod ) {

			// =================================================================
			if(driver.getTitle().equals(title)){
				logger.info("in :" + UtilKit.currentMethod() + "Page title : " + driver.getTitle());
				return true;

			}
			UtilKit.suspendAction(250);;
			currentWait = currentWait + (float)0.25;
		}
		
		logger.warn("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + "Wait for title to load expired");
		return false;
	}	


	public static Object[][] getTestData(String project, String application, String testCase) {

		/*
		 * This is here because this module is called before the init() module
		 * for test cases that specify a data Provider If thats the case, we
		 * need to initialize Logger here before we can use it
		 */
		if (projectName.isEmpty())
			projectName=project;
		if (logger == null) {
			UtilKit.createLogger();
		}
		UtilKit.loadDBProperties(application); // Load the DB properties

		// If a SQL statement is not provided for this This test case then use Excel Master Data file
		// Otherwise use the SQL Database
		logger.info("BP PROP : " + application.toUpperCase() +"_" + testCase.toUpperCase() + "_SQL");
		if (getDBProp(application.toUpperCase() +"_" + testCase.toUpperCase() + "_SQL") == null) {
			return getExcelTestData(application, testCase);

		} else {
			return getSQLTestData(application, testCase);
		}
	}

	private static Object[][] getSQLTestDataOld(String application, String testCase) {

		// Connect to the SQL Database if we are not connected yet
		if (dbConn == null) {
			if (!dbConnect()) {
				logger.fatal("DB Connection Failure : " + getDBProp("DBCONN_URL") + " Exiting Test .........");
				System.exit(10);
			}
		}

		/*
		 * At this moment: login and Order search can use data from the database
		 * Other test scenarios will continue to use the Excel file Each of
		 * those modules returns a Object[][]
		 */
		if (testCase.equalsIgnoreCase("login")) {
			return getLoginSQLTestData(application, testCase);
		}

		if (testCase.equalsIgnoreCase("orderSearch")) {
			return getOrderSearchSQLTestData(application, testCase);
		}

		// Get the data from Excel is the default action
		return getExcelTestData(application, testCase);

	}

	private static Object[][] getSQLTestData(String application, String testCase) {

		try {
			// Connect to the SQL Database if we are not connected yet
			if (dbConn == null) {
				if (!dbConnect()) {
					logger.fatal("DB Connection Failure : " + getDBProp("DBCONN_URL") + " Exiting Test .........");
					System.exit(10);
				}
			}

			Statement st = dbConn.createStatement();
			// get the sql statement text
			String stString = UtilKit.getDBProp(application.toUpperCase() + "_" + testCase.toUpperCase() + "_SQL");
			logger.info("Executing SQL :" + stString);

			// Parse the columns names from the SQL statement
			String columnNames[] = UtilKit.parseColumnsNew(stString);
			logger.info("Column Names :" + columnNames);

			ResultSet rs = st.executeQuery(stString);
			ResultSetMetaData rsmd = rs.getMetaData();
			// int rowCount = rs.getFetchSize();

			// To calculate the row count you will have to access the last row
			// and
			// get its row number
			rs.last();
			int rowCount = rs.getRow();
			rs.beforeFirst(); // back to the beginning

			int columnsCount = rsmd.getColumnCount();
			logger.info("SQL Statement Excecuted :" + " Rows :" + rowCount + "  columns :" + columnsCount);

			Object[][] testDataArray = new Object[rowCount][columnsCount]; // To
																			// be
																			// returned
			// For each Row load every column. based on the column names Array
			for (int i = 0; i < rowCount; i++) {
				rs.next();
				for (int j = 0; j < columnNames.length; j++) {
					logger.info("In : " + Thread.currentThread().getStackTrace()[1].getMethodName() + " DB Loading : "
							+ columnNames[j] + " : " + rs.getString(j +1)/*rs.getString(columnNames[j])*/);
					testDataArray[i][j] = rs.getString(j+1);
				}
			}
			return testDataArray;

		} catch (SQLException e) {
			logger.fatal("Sql Exception generated ");
			e.printStackTrace();
			System.exit(10);
		}

		// Get the data from Excel is the default action
		return getExcelTestData(application, testCase);

	}

	// Gets order search data from the database
	private static Object[][] getOrderSearchSQLTestData(String application, String testCase) {
		try {
			Statement st = dbConn.createStatement();

			// The sql statement is stored as a DB Property
			String stString = UtilKit.getDBProp(application.toUpperCase() + "_" + testCase.toUpperCase() + "_SQL");
			logger.info("Executing SQL :" + stString);

			// Execute Query, get the ResultSet and the Metadata
			ResultSet rs = st.executeQuery(stString); // ResultSet stores the
														// result of the query
			ResultSetMetaData rsmd = rs.getMetaData(); // The results Metadata
														// is part of the
														// results

			// int rowCount = rs.getFetchSize(); //Not needed in the case

			// To calculate the row count you will have to access the last row
			// and get its row number

			rs.last();
			int rowCount = rs.getRow();

			// Point back to the begining
			rs.beforeFirst();

			// Get the columns count from the Metadata
			int columnsCount = rsmd.getColumnCount();

			logger.info("SQL Statement Excecuted :" + " Rows :" + rowCount + "  columns :" + columnsCount);

			Object[][] testDataArray = new Object[rowCount][columnsCount]; // Instantiate
																			// the
																			// returning
																			// Object
																			// Data
																			// Array

			for (int i = 0; i < rowCount; i++) {
				rs.next();
				logger.info("in : " + Thread.currentThread().getStackTrace()[1].getMethodName() + " DB Loading :"
						+ rs.getString("orderId"));
				testDataArray[i][0] = rs.getString("orderId");
			}
			return testDataArray;

		} catch (SQLException e) {
			logger.fatal("Sql Exception generated ");
			e.printStackTrace();
			System.exit(10);
		}
		return null;
	}
	
	public static String [] parseColumnsNew(String sqlStmtStr){

		CCJSqlParserManager parseManager = new CCJSqlParserManager();
		StringReader sqlStmtRead = new StringReader(sqlStmtStr);
		String [] columnsNames = null;

		try {
			// Need to be specific here since the is also the Java SQL Statement in this class 
			net.sf.jsqlparser.statement.Statement parsedStmt = parseManager.parse(sqlStmtRead);
			
			Select selectStmt = (Select)parsedStmt;
			
			SelectBody selectBody = selectStmt.getSelectBody();
			
			PlainSelect plainSelect = (PlainSelect)selectBody;
			List<SelectItem> selectedItems = plainSelect.getSelectItems();
			columnsNames = new String[selectedItems.size()]; // The first string is not used, is just space
			for (int i = 0; i < selectedItems.size(); i++){
				columnsNames[i] = selectedItems.get(i).toString();
				System.out.println("Selected Item : " + selectedItems.get(i).toString());
			}

			System.out.println("==================================================================================");
		} catch (JSQLParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return columnsNames;

	}
	
	private static String [] parseColumns(String sqlStm){
		
		String mySqlStm = sqlStm.toLowerCase();

		
		String secondPart = mySqlStm.replaceFirst("select", ""); // Replace "Select"
		logger.info("secondPart :" + secondPart);
		
		String [] rawColumns = secondPart.split("from"); // Cut after "From"
		logger.info("rawColumns :" + rawColumns[0] + ":");
		
		String plainColumns = rawColumns[0].replace(",", "");
		logger.info("plainColumns :" + plainColumns +":");
		
		
		String columnsArray[] = plainColumns.split(" "); // Split on space
		String [] columnsNames = new String[columnsArray.length -1]; // The first string is not used, is just space
		
		for (int i = 0; i < columnsArray.length; i++){
			// Discard the first String after the split. it only contains a space.
			if (i == 0) continue;
			columnsNames[i -1]  =  columnsArray[i];
		}

		for (String myColumn: columnsNames ){
			logger.info("column :" + myColumn + ":");
		}
		
		return columnsNames;
		
	}

	// Gets login data from the database
	private static Object[][] getLoginSQLTestData(String application, String testCase) {
		try {
			Statement st = dbConn.createStatement();
			String stString = UtilKit.getDBProp(application.toUpperCase() + "_" + testCase.toUpperCase() + "_SQL");
			logger.info("Executing SQL :" + stString);
			ResultSet rs = st.executeQuery(stString);
			ResultSetMetaData rsmd = rs.getMetaData();
			// int rowCount = rs.getFetchSize();

			// To calculate the row count you will have to access the last row
			// and
			// get its row number
			rs.last();
			int rowCount = rs.getRow();
			rs.beforeFirst();
			int columnsCount = rsmd.getColumnCount();
			logger.info("SQL Statement Excecuted :" + " Rows :" + rowCount + "  columns :" + columnsCount);
			Object[][] testDataArray = new Object[rowCount][columnsCount];
			for (int i = 0; i < rowCount; i++) {
				rs.next();
				logger.info("in : " + Thread.currentThread().getStackTrace()[1].getMethodName() + " DB Loading : "
						+ rs.getString("userName") + "  " + rs.getString("passwd"));
				testDataArray[i][0] = rs.getString("userName");
				testDataArray[i][1] = rs.getString("passwd");
			}
			return testDataArray;

		} catch (SQLException e) {
			logger.fatal("Sql Exception generated ");
			e.printStackTrace();
			System.exit(10);
		}
		return null;
	}

	/*
	 * This Module makes the necessary Apache POI calls needed to get the test
	 * data from the Excel file into the Object[][] being returnrd to the Testng
	 * Data provider calls include XSSFWorkbook(), XSSFSheet(), XSSFRow() etc
	 */
	public static Object[][] getExcelTestData(String application, String testCase) {

		/*
		 * This is here because this module is called before the init() module
		 * for test cases that specify a data Provider If thats the case, we
		 * need to initialize Logger here before we can use it
		 */
		if (logger == null) {
			UtilKit.createLogger();
		}

		// XSSFWorkbook requires an InputStream as a parameter
		FileInputStream testF = null;
		try {
			testF = new FileInputStream(projectFolder + "/" + projectName + "/" +  application + resourcesFolder + "/MasterTestData.xlsx");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		XSSFWorkbook wb = null;
		try {
			wb = new XSSFWorkbook(testF); // Create the Workbook
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XSSFSheet sheet = wb.getSheet(application); // Get the Sheet in the
													// Workbook
		logger.info("sheet rows count :" + sheet.getLastRowNum());

		// Instantiate the list of XSSF rows
		ArrayList<XSSFRow> caseList = new ArrayList<XSSFRow>();

		caseList = UtilKit.findRows(sheet, testCase); // finds the list of rows
														// matching
		// the given test case String

		XSSFRow titlesRow = caseList.get(0); // The first row is the column
												// title, It will be ignored.

		// Allocate memory for the Data array. The -1 us used in both dimensions
		// since the column title row itself is not loaded.
		Object[][] testDataArray = new Object[caseList.size() - 1][(titlesRow.getLastCellNum()) - 1];
		logger.info(
				"testDataArray Allocation [" + (caseList.size() - 1) + "][" + (titlesRow.getLastCellNum() - 1) + "]");

		// This loop loads each cell into the test Data array
		for (int i = 0; i < (caseList.size() - 1); i++) {
			XSSFRow currRow = caseList.get(i + 1);
			for (int j = 0; j < (currRow.getLastCellNum()) - 1; j++) {
				logger.info("Loading " + currRow.getCell(j + 1).toString());
				testDataArray[i][j] = currRow.getCell(j + 1).toString();
			}

		}

		try {
			wb.close(); // Close the workbook
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger = null;
		return testDataArray;

	}

	public static ArrayList<XSSFRow> findRows(XSSFSheet sheet, String findStr) {

		ArrayList<XSSFRow> rowList = new ArrayList<XSSFRow>();// Will store the
																// list of rows
																// in the given
																// sheet

		/*
		 * TODO - Make the iterator work latter
		 * 
		 * Iterator<Row> rowIterator = sheet.iterator();
		 * while(rowIterator.hasNext()){ XSSFRow currRow = (XSSFRow)
		 * rowIterator.next(); uLog('D',"Checking Row : " + currRow.toString());
		 * if(currRow.getCell(1).getStringCellValue().equalsIgnoreCase(findStr))
		 * {
		 * 
		 * uLog('I', "Loading Row : " + currRow.toString());
		 * rowList.add(currRow); } }
		 */

		/*
		 * Loop thru every row in the sheet and store in the list the ones that
		 * match the findStr
		 */
		for (int i = 0; i < (sheet.getLastRowNum() + 1); i++) {
			XSSFRow currRow = sheet.getRow(i);
			if (currRow != null) {
				logger.info("Checking Cell : " + currRow.getCell(0).getStringCellValue());
				// Look at the first cell/column in each row, if it matches
				// findStr
				// then add it to the row list
				if (currRow.getCell(0).getStringCellValue().equalsIgnoreCase(findStr)) {
					logger.info("Reading " + findStr + " Row Number: " + currRow.getRowNum());
					rowList.add(currRow);
				}
			}

		}

		return rowList;
	}

	// Scroll Down to the end of the page
	public static void scrollDown(WebDriver driver) {
		Actions myActions = new Actions(driver);
		myActions.sendKeys(Keys.PAGE_DOWN);
		myActions.sendKeys(Keys.END);
		myActions.build().perform();
	}

	public static void suspendAction(long milliseconds){
		
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void scrollDownUnit(HtmlUnitDriver driver) {
		Actions myActions = new Actions(driver);
		myActions.sendKeys(Keys.PAGE_DOWN);
		myActions.build().perform();
	}

	// Scroll to the location of the given Element
	public static void scrollToElement(WebElement element, WebDriver driver) {
		Point elementCoordinates = element.getLocation();
		// This is done by executing a javaScript
		String scriptContent = new String(
				"window.scrollTo(" + elementCoordinates.getX() + "," + (elementCoordinates.getY() - 40) + ")");
		UtilKit.executeJavascript(driver, scriptContent);

	}
	// Scroll to a give Point 
	public static void scrollToPoint(Point coordinates, WebDriver driver) {
		// This is done by executing a javaScript
		String scriptContent = new String(
				"window.scrollTo(" + coordinates.getX() + "," + (coordinates.getY()) + ")");
		logger.info("Scrolling to :" + coordinates.getX() + "," + coordinates.getY());
		UtilKit.executeJavascript(driver, scriptContent);

	}

	public static void scrollToElementUnit(WebElement element, HtmlUnitDriver driver) {
		Point elementCoordinates = element.getLocation();
		// This is done by executing a javaScript
		String scriptContent = new String(
				"window.scrollTo(" + elementCoordinates.getX() + "," + (elementCoordinates.getY() - 40) + ")");
		UtilKit.executeJavascript(driver, scriptContent);

	}

	// this will return thr calling method name
	public static String currentMethod() {
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}

	// print the stack and log the exception message
	public static String exceptionLogger(Exception e) {

		String excLog = new String();

		e.printStackTrace();

		String msg = (e.getMessage());
		if (!msg.isEmpty())
			excLog = excLog + "\n\n EXCEPTION MESSAGE : \n" + msg + "\n\n";
		logger.error(excLog);

		return excLog;

	}

	public static void uLog(char level, String msg) {

		// Levels (FATAL, ERROR, WARN, INFO, DEBUG)
		switch (level) {
		case 'F':
			logger.fatal(msg);
			break;
		case 'E':
			logger.error(msg);
			break;
		case 'W':
			logger.warn(msg);
			break;
		case 'I':
			logger.info(msg);
			break;
		case 'D':
			logger.info(msg);
			break;
		}
	}

	public static String getDateTime() {
		SimpleDateFormat dateTimeForm = new SimpleDateFormat("MM/dd/YYYY HH:mm:ss");
		Date myDateTime = Calendar.getInstance().getTime();

		return (dateTimeForm.format(myDateTime));
	}

	public static String getPlainDateTime() {
		SimpleDateFormat dateTimeForm = new SimpleDateFormat("YYYYMMddHHmmss");
		Date myDateTime = Calendar.getInstance().getTime();

		return (dateTimeForm.format(myDateTime));
	}

	public static Object executeJavascript(WebDriver driver, String javaScript) {
		/*
		 * Cast the driveras JavaScriptExecutor to be able to call
		 * executeScript()
		 */
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (js.executeScript(javaScript));
	}

	public static boolean executeBooleanJavascript(WebDriver driver, String javaScript) {
		/*
		 * Cast the driver JavaScriptExecutor to be able to call
		 * executeScript()
		 */
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (Boolean) (js.executeScript(javaScript));
	}

	public static void executeJavascriptOnElement(WebDriver driver, String javaScript, WebElement element) {
		/*
		 * Cast the driver JavaScriptExecutor to be able to call
		 * executeScript()
		 */
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript(javaScript, element);
		UtilKit.logger.info("javaScript Excecuted : " + "|" + javaScript + "|");
	}
	
	public static void javaScriptClick(WebDriver driver, WebElement element){
		UtilKit.executeJavascriptOnElement(driver, "arguments[0].click();", element);
		
	}
	public static void javaScriptSendKeys(WebDriver driver, WebElement element, String keysString){
		UtilKit.executeJavascriptOnElement(driver, "arguments[0].setAttribute('value'," + "'" + keysString + "')", element);
		
	}
	public static String javaScriptGetValue(WebDriver driver, WebElement element){
		
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (String) js.executeScript("return arguments[0].getAttribute('value')", element);
		
	}
	public static String javaScriptGetClass(WebDriver driver, WebElement element){
		
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (String) js.executeScript("return arguments[0].getAttribute('class')", element);
		
	}
	public static String javaScriptGetHTML(WebDriver driver, WebElement element){
		
		JavascriptExecutor js = (JavascriptExecutor) driver;
		//return (String) js.executeScript("return arguments[0].getAttribute('outerHTML')", element);
		return (String) js.executeScript("return arguments[0].outerHTML", element);
		
	}

	// Using the java AWT robot class.
	// It will type in the characters in the given Char [] (one by one)
	// It will only handle 3 special characters (\ : _) since it was coded
	// For typing directory paths
	public static void arrayKeyPress(Robot myRobot, char[] myArray) {
		for (int i = 0; i < myArray.length; i++) {
			if (myArray[i] == '\\') {
				myRobot.keyPress('\\');
				myRobot.keyRelease('\\');
			} else if (myArray[i] == ':') {
				myRobot.keyPress(KeyEvent.VK_SHIFT);
				myRobot.keyPress(KeyEvent.VK_SEMICOLON);
				myRobot.keyRelease(KeyEvent.VK_SEMICOLON);
				myRobot.keyRelease(KeyEvent.VK_SHIFT);
			} else if (myArray[i] == '_') {
				myRobot.keyPress(KeyEvent.VK_SHIFT);
				myRobot.keyPress(KeyEvent.VK_MINUS);
				myRobot.keyRelease(KeyEvent.VK_MINUS);
				myRobot.keyRelease(KeyEvent.VK_SHIFT);
			} else {
				myRobot.keyPress(myArray[i]);
				myRobot.keyRelease(myArray[i]);
			}
		}
	}

	/*
	 * Perform a boolean returning file operation like delete or change the last
	 * modification date on the given filename
	 */
	public static Boolean fileOperation(String fileName, char operation) {

		File myFile = new File(fileName);
		// As a switch to allow for more operations in the future
		switch (operation) {
		case 'D':
			return myFile.delete();
		case 'M':
			return myFile.setLastModified(startTime);
		case 'N':
			return myFile.exists();
		}
		return false;
	}
	
	public static int findFrameIndex(WebDriver driver, List<WebElement> framesList, By elementLocator) {

		boolean inChildFrame = false;
		
		//Try finding it in the current context
		if(driver.findElements(elementLocator).size() > 0){
			UtilKit.logger.info("Element : " + elementLocator.toString() + "Found in Current Context... URL: " + driver.getCurrentUrl());
			return 0;
		}
		for (int i = 0; i < framesList.size(); i++) {
			try {
				if(inChildFrame == true){
					driver.switchTo().parentFrame();
					inChildFrame = false;
				}
				driver.switchTo().frame(framesList.get(i));
				UtilKit.suspendAction(1000);
				inChildFrame = true;
				if(framesList.get(i).findElements(elementLocator).size() > 0){
					UtilKit.logger.info("Element :" + elementLocator.toString() + " found in Frame : " + framesList.get(i).getAttribute("outerHTML"));
					driver.switchTo().parentFrame();
					return (i+1);
				}
				else
					UtilKit.logger.info("Element :" + elementLocator.toString() + " Not found in Frame : " + framesList.get(i).getAttribute("outerHTML"));
					
			} catch (Exception e) {
				UtilKit.logger.info("Ignored Exeption In :" + UtilKit.currentMethod() + " : " + e.getMessage());
				e.printStackTrace();
				continue;
			}
		}
		if(inChildFrame == true)
			driver.switchTo().parentFrame();
		UtilKit.logger.info("Element :" + elementLocator.toString() + " Not found in Current Context or Frame ");
		return -1;
	}
		
		
	public static List<WebElement> findFrames(WebDriver driver) {
		
		List<WebElement> allFramesList = new ArrayList<WebElement>();
		int i = 0;
		int j = 0; //allFramesList index
		try {
			logger.info("Finding Frames in Page Title : " + driver.getTitle());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<WebElement> frameList = driver.findElements(By.tagName("frame"));
		logger.info("----------------------Frame count : " + frameList.size()
				+ "------------------------------------------------------");
		for (WebElement element : frameList) {
			allFramesList.add(j, element); j++;
			logger.info("Frame No: " + i + " Frame Title: " + element.getAttribute("title") + "  Frame Name : "
					+ element.getAttribute("name"));
			logger.info("Frame HTML: " + element.getAttribute("outerHTML"));
			logger.info("----------------------------------------------------------------------------");
			i++;
		}

		i = 0;
		List<WebElement> iframeList = driver.findElements(By.tagName("iframe"));
		logger.info("----------------------iFrame count : " + iframeList.size()
				+ "------------------------------------------------------");
		for (WebElement element : iframeList) {
			allFramesList.add(j, element); j++;
			logger.info("iFrame No: " + i + " iFrame Title: " + element.getAttribute("title")
					+ "  iFrame Name : " + element.getAttribute("name"));
			logger.info("iFrame HTML: " + element.getAttribute("outerHTML"));
			logger.info("----------------------------------------------------------------------------");
			i++;
		}

		i = 0;
		List<WebElement> frameSetList = driver.findElements(By.tagName("frameset"));
		logger.info("----------------------FrameSet count : " + frameSetList.size()
				+ "------------------------------------------------------");
		for (WebElement element : frameSetList) {
			allFramesList.add(j, element); j++;
			logger.info("FrameSet No: " + i + " FrameSet Title: " + element.getAttribute("title")
					+ "  Frame Set Name : " + element.getAttribute("name"));
			logger.info("FrameSet HTML: " + element.getAttribute("outerHTML"));
			logger.info("----------------------------------------------------------------------------");
			i++;
		}
		
		return allFramesList;
	}
	
	/**
	 * @param myElement
	 */
	public static void highLiteElement(WebElement myElement, Actions mouseActions) {
		Point eLocation = myElement.getLocation();
		Rectangle eRect = myElement.getRect();
		System.out.println("Element to highlite found : " + myElement.getText() + " Loc :" + eLocation.toString() + 
				             " Rect : " + eRect.getX() + "," + eRect.getY() + "," + eRect.getWidth() + "," + eRect.getHeight());
		System.out.println(" Dimension : " + eRect.getDimension().toString());
		mouseActions.moveToElement(myElement, (eRect.getWidth() / 2) * -1, 0);
		UtilKit.suspendAction(100);
		mouseActions.clickAndHold().moveByOffset(eRect.getWidth(), 0).build().perform();
		mouseActions.release().build().perform();
		UtilKit.suspendAction(50);
	}
	
	public static void printCurrentPageHTML(WebDriver driver){
		System.out.println("\n================================================================================\n");
		System.out.println("\t\t\tPage URL : " + driver.getCurrentUrl() +"\n");
		System.out.println("\t\t\tPage Title : " + driver.getTitle() + "\n");
		System.out.println("\t\t\tPage source :\n" + driver.getPageSource() +"\n");
		System.out.println("\n================================================================================\n");
	}
	
	public static boolean checkAlertMessage(WebDriver driver, String message) {

		int counter = 0;
		while (counter < 3) {
			try {
				Alert myAlert = driver.switchTo().alert();
				UtilKit.logger.info("Alert Text : " + myAlert.getText());
				if (myAlert.getText().contains(message))
					return true;
			} catch (Exception e) {
				UtilKit.suspendAction(1000);
				counter++;
				continue;
			}
		}
		return false;
	}
	
	public static void sendRestResultsEMail( String resultsMessage){
		
//		SMTPHOST = smtp.gmail.com
//		SMTPPROTOCOL = smtps
//		SMTP.STARTTLS.ENABLE = true
//		SMTP.AUTH = true
//		SMTPPORT = 465 
//		FROM = efrain583@gmail.com
//		TO = efrain583@yahoo.com
//		SMTPUSERNAME = efrain583
//		SMTPPASSWORD = efrago1= 
//		SUBJECT = Test Errors
//		BUFFERSIZE = 512
//		SMTPDEBUG = true
		
		final String userName = UtilKit.getConfigProp("SMTPUSERNAME");
		//final String userName = "efrain583";
		final String password = UtilKit.getConfigProp("SMTPPASSWORD");
//		final String password = "efrago1=";

		Properties props = new Properties();
		//props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.auth", UtilKit.getConfigProp("SMTP.AUTH"));
		//props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.starttls.enable", UtilKit.getConfigProp("SMTP.STARTTLS.ENABLE"));
//		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.host", UtilKit.getConfigProp("SMTPHOST"));
//		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.port", UtilKit.getConfigProp("SMTPPORT").toString());

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password);
			}
		  });

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(UtilKit.getConfigProp("FROM")));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(UtilKit.getConfigProp("TO")));
			message.setSubject("Test Results - Do Not Reply");
//			message.setText("Test Team," + "\n\n" + resultsMessage  
//				+ "\n\n Do Not Reply, please!");

			// Create the message part
	         BodyPart messageBodyPart = new MimeBodyPart();

	         // Now set the actual message
			messageBodyPart.setText("Test Team," + "\n\n" + resultsMessage  
				+ "\n\n Do Not Reply, please!");

	         // Create a multipar message
	         Multipart multipart = new MimeMultipart();

	         // Set text message part
	         multipart.addBodyPart(messageBodyPart);

	         // Part two is attachment
	         messageBodyPart = new MimeBodyPart();
	         String filename = "/Users/efrain/git/in.automationtest/README.md";
	         DataSource source = new FileDataSource(filename);
	         messageBodyPart.setDataHandler(new DataHandler(source));
	         messageBodyPart.setFileName(filename);
	         multipart.addBodyPart(messageBodyPart);

	         // Send the complete message parts
	         message.setContent(multipart);

	         // Send message
	         Transport.send(message);

	         System.out.println("Sent message successfully....");	
			

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
