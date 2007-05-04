/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CreateTestXML.java,v 1.1 2007-05-04 20:47:43 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.authentication;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <code>CreateTestXML</code> is a helper class to create the XML file for the 
 * form based validation for each of the forms.
 * This is xml used by the <code>WebTest</code> to verify the test.
 */
public class CreateTestXML{
    
     public static String newline = System.getProperty("line.separator");
     public static String fileseparator = System.getProperty("file.separator");
     private String fileName;
   
   /**
    * Default constructor
    */
    public CreateTestXML(){
        
    }
    
    /**
     * Creates the service based form Login XML
     * @param Map contains test related data
     * @param true if is is negative test
     * @return xml file name
     */
    public String createServiceXML(Map testMap,boolean testNegative) 
    throws Exception {
        String users = (String)testMap.get("users");
        String passMsg = (String)testMap.get("passmessage");
        String failMsg = (String)testMap.get("failmessage");
        String testURL = (String)testMap.get("url");
        String baseDirectory = (String)testMap.get("baseDir");
        String loginService = (String)testMap.get("servicename");
        if(!testNegative){
            fileName = baseDirectory + fileseparator + "built" + fileseparator +
                    "classes" + fileseparator + loginService + "-positive.xml";
        }else{
            fileName = baseDirectory + fileseparator + "built" + fileseparator + 
                    "classes" + fileseparator + loginService + "-negative.xml";
        }
        PrintWriter out = new PrintWriter(new BufferedWriter
                (new FileWriter (fileName)));
        out.write("<url href=\"" + testURL + "/UI/Login?service=" +
                loginService);
        out.write("\">");
        out.write(newline);
        StringTokenizer test_users = new StringTokenizer(users,"|");
        List<String> test_userList = new ArrayList<String>();
        int tokennumber = test_users.countTokens();
        while (test_users.hasMoreTokens()){
            test_userList.add(test_users.nextToken());
        }
        int totalforms = test_userList.size();
        int formcount = 0;
        for (String test_userName: test_userList){
            formcount = formcount +1;
            String tuser;
            String tpass;
            int uLength = test_userName.length();
            int uIndex = test_userName.indexOf(":");
            tuser= test_userName.substring(0,uIndex);
            tpass = test_userName.substring(uIndex+1,uLength);
            out.write("<form name=\"Login\" buttonName=\"\" >");
            out.write(newline);
            out.write("<input name=\"IDToken1\" value=\"" + tuser + "\" />");
            out.write(newline);
            out.write("<input name=\"IDToken2\" value=\""
                + tpass + "\" />");
            out.write(newline);
            if(formcount == totalforms){
                if(!testNegative){
                out.write("<result text=\"" + passMsg + "\" />");
                }else {
                out.write("<result text=\"" + failMsg + "\" />");
                }
                out.write(newline);
                out.write("</form>");
                out.write(newline);
                out.write("</url>");
                out.write(newline);
            }
            else {
                out.write("</form>");
                out.write(newline);
            }
        }
        out.flush();
        out.close();
        
        return fileName;

    }
}
