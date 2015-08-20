package com.spillman;

import javax.xml.rpc.ServiceException;

import com.atlassian.jira.rpc.exception.RemoteAuthenticationException;
import com.atlassian.jira.rpc.exception.RemoteException;
import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.atlassian.jira.rpc.soap.beans.RemoteProject;
import com.spillman.jira.rpc.soap.jirasoapservice_v2.JiraSoapService;
import com.spillman.jira.rpc.soap.jirasoapservice_v2.JiraSoapServiceServiceLocator;

public class TestJiraSoapClient {

	public static void main(String[] args) {
		JiraSoapServiceServiceLocator jssl = new JiraSoapServiceServiceLocator();
		JiraSoapService jss;
		try {
			jss = jssl.getJirasoapserviceV2();
			
			System.out.println("Logging in");
			String token = jss.login("chellewell", "jira");
			System.out.println("Login returned token " + token);
			
			for (String key : keys) {
				System.out.print("deleting project " + key + "...");
				jss.deleteProject(token, key);
				System.out.println("done");
			}
			System.out.println("All projects have been deleted.");
			
//			System.out.println("Getting issue P-6538");
//			RemoteIssue issue = jss.getIssue(token, "P-6538");
//			System.out.println("Summary: " + issue.getSummary());
//			
//			System.out.println("Attempting to create a project");
//			RemoteProject proj = jss.createProject(token, "CHRIS", "Test Soap Project", "This was created via a soap client", "", "chellewell", null, null, null);
//			System.out.println("Project created: " + proj.getKey());
//			
//			System.out.println("Logging out");
//			jss.logout(token);
//			System.out.println("Done");
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (RemoteAuthenticationException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (java.rmi.RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private final static String[] keys = {
		"UPGRADETAA",
		"ACTIVEDIAA",
		"ACTIVEDIAB",
		"ADDAGENCAA",
		"ADDBOOKIAA",
		"ADDBOOKIAB",
		"ADDFIPSEAA",
		"ALECITATAA",
		"ALECRASHAA",
		"ARKANSASAA",
		"BCSCODEFAA",
		"BLOOMINGAA",
		"BROWNSVIAA",
		"CACITATIAA",
		"CACRASHFAA",
		"CAUCRREPAA",
		"CALPHOTOAA",
		"CANTEENIAA",
		"CASECLOSAA",
		"CISCOGPSAA",
		"COMPUTECAA",
		"DATAREPLAA",
		"DETAILEDAA",
		"DEVICETEAA",
		"EFRCITATAA",
		"EFRCRASHAA",
		"EFRMOBILAB",
		"EFRMOBILAC",
		"EFRMOBILAA",
		"EFROFFLIAA",
		"EFRSIGNTAA",
		"EFRVERIFAA",
		"EVIDENCEAA",
		"GCRTAORAAA",
		"GEARSECRAA",
		"ILAPSINTAA",
		"INUCRHATAA",
		"INCODECOAA",
		"INDIANAUAB",
		"INDIANAUAA",
		"JTACECWSAA",
		"KANSASVOAA",
		"KIBRSGATAA",
		"LACOUNTYAA",
		"LOCUTIONAA",
		"LOGISYSCAA",
		"MAIBRAA",
		"MAIBRUPDAA",
		"MACHFIREAA",
		"MAKESCHEAA",
		"MESTATELAA",
		"MONTGOMEAB",
		"MONTGOMEAA",
		"MOTOROLAAA",
		"NEEDTHEAAA",
		"NETGEARUAA",
		"NEWDAWNCAA",
		"NYLIVESCAA",
		"NYSTATELAA",
		"OAKRIDGEAA",
		"OFFENSESAA",
		"OHCRASHFAA",
		"OKIBRAA",
		"OKSTATELAA",
		"OKEECHOBAA",
		"ONGOINGSAA",
		"ONLINECRAA",
		"ORANGECOAA",
		"PASTATELAA",
		"PATRACSCAB",
		"PATRACSCAA",
		"PAUCRAA",
		"PORTOLLEAA",
		"PORTLANDAA",
		"PRINTRECAA",
		"PUYALLUPAA",
		"REDDINGSAA",
		"REDOSENTAA",
		"RELEASECAA",
		"RELEASECAB",
		"REPLACEMAA",
		"REWRITEWAA",
		"SLIVEMIGAA",
		"SCIBRUPDAA",
		"SCECRASHAA",
		"SENTRYXMAA",
		"SEPARATEAA",
		"SPILLMANAB",
		"SPILLMANAA",
		"STATELINAA",
		"SUMMITUIAA",
		"TESTDLSCAA",
		"TESTGETAAA",
		"TESTGPSEAA",
		"TESTMOTIAA",
		"TESTPANAAA",
		"TESTPATRAA",
		"TESTTHEVAA",
		"TESTTWOMAA",
		"TESTUNSUAA",
		"TEXASBAIAA",
		"TEXASUCRAA",
		"TNIBRAA",
		"TNSTATELAA",
		"TXCRASHFAA",
		"UPDATEAVAA",
		"UPDATEGEAA",
		"UPDATEORAA",
		"UPGRADELAA",
		"UTCITATIAA",
		"VEHICLEMAA",
		"WAIBRUPDAA",
		"WAPROTECAA",
		"WASHINGTAA",
		"WEBERUTDAA",
		"WESTVIRGAA",
		"WIPROTECAA",
		"WVIBRAA",
		"WYREPORTAA",
		"ZOLLRESCAA"
	};
}
