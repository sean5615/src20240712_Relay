package Webservice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import Model.CTBCPaymentNotice;
import Util.CTBC_errcode;
import Util.MyProperties;
import Util.SSLUtil;

public class CTBCWebservice {
	//protected MyLogger logger = null;
		
		private final String ORGID = "8814602644";
//		private final String NOU_URL = "http://atap.nou.edu.tw/";
//		private final String NOU_URL = "http://localhost:9999/";
		private final String SCHOOL_NAME = "國立空中大學";
		private final String SCHOOL_NAME_2 = "國立空中大學推廣教育中心";
	
		private HttpURLConnection connection;
	    private HttpsURLConnection urlConnection;
	    private URL url;
	    
	    
	    /**中國信託近來統一走這**/
	    public String DoCtbc(String QueryString) {
	    	String result = "";
	    	String errcode = "0000";
	    	String op = "";
	    	
	    	Logger logger  =  Logger.getLogger("DoCtbc");
            logger.debug("QueryString  = " + QueryString);
            
            try{
            	if("".equals(QueryString) || QueryString == null) {
	        		errcode = "S999";
	        	} 
            	
            	JSONObject jb = XML.toJSONObject(QueryString);            	
            	
            	try{
            		jb = jb.getJSONObject("NOU");
            		op = jb.getString("op");
            	} catch(Exception ex) {
            		errcode = "S999";
            	}
            	
            	System.out.println("op = " + op);
            	
            	if("Q0002".equals(op)) {
            		result = Q0002(jb.toString());
            	} else if("T0001".equals(op)) {
            		result = T0001(jb.toString());
            	} else {
            		errcode = "S999";
            		throw new Exception("方法對不上!");
            	}
          
            } catch(Exception ex) {
            	JSONObject jsonObject = new JSONObject();
                
                try {
					jsonObject.put("op", op);
					jsonObject.put("errcode", errcode);
	                jsonObject.put("schoolname", SCHOOL_NAME);
	                jsonObject.put("studentname", "");
	                jsonObject.put("amount", "");
	                jsonObject.put("payduedate", "");
				} catch (JSONException e) {
					e.printStackTrace();
				}                
                
                Iterator it = jsonObject.keys();
    			StringBuffer sb = new StringBuffer();
    			while(it.hasNext()){
    				String key = (String) it.next();
    				String value = jsonObject.optString(key);
    				sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
    			}
                
                result = "<NOU>" +  sb.toString() + "</NOU>";
            	ex.printStackTrace();
            }
	    	return result;
	    }
	    
	    
	    /** 從空大呼中繼站把交易代碼 機關代號 使用者pp 銷帳編號帶過去愛繳費 **/
	    public String Q0002(String QueryString) {
	    	String result = "";
	    	String errcode = "0000";
	    	//PropertyConfigurator.configure("D:/Workspace/JAVA/Nou/NouRelayStation_web/axis2/WebContent/WEB-INF/classes/log4j.properties");
            Logger logger  =  Logger.getLogger("Q0002 Start");
            logger.debug("Q0002 queryString = " + QueryString); 
            
            String NOU_URL = "https://noustud.nou.edu.tw/";
	    	
	    	try{
	    		NOU_URL = MyProperties.getProperties("NOU_URL");
	    	} catch(Exception e) {
	    		logger.debug("取得參數'NOU_URL'時發生錯誤");
	    		e.printStackTrace();
	    	}
            
	    	try{
	    		if("".equals(QueryString) || QueryString == null) {
	        		errcode = "S999";
	        	}    	
	    		
	    		String op = "";
				String orgid = "";
				String pwd = "";			
				String rid = "";
				
//    			JSONObject jb = XML.toJSONObject(QueryString);
    			JSONObject jb = new JSONObject(QueryString);
    			
            	try{
            		//jb = jb.getJSONObject("NOU");
            		op = jb.getString("op");
					orgid = jb.get("orgid").toString();
					pwd = jb.getString("pwd");
					rid = jb.get("rid").toString();
            	} catch(Exception ex) {
            		errcode = "S999";
            	}
				
				logger.debug("op = " + op);
				logger.debug("orgid = " + orgid);
				logger.debug("pwd = " + pwd);
				logger.debug("rid = " + rid);
				
				//這邊做檢核
				if(!"Q0002".equals(op)) {
					errcode = "S999";
				}
				
				//空大學雜費及推廣教育中心 中國信託i繳費進來同一IP，故移除該判斷20240712 maggie
				//if(!ORGID.equals(orgid)) {
				//	errcode = "C002";
				//}
				
				if(!"NOU".equals(pwd)) {
					errcode = "C003";
				}
				
				if("".equals(rid) || rid.trim().length() == 0) {
					errcode = "2001";
				}
				
	    		String sendStr = "";
	    		
	    		//開始準備資料傳送到CTBC
	    		//先回系統撈該銷帳編號相關資料
	    		String urlStr = "";
				urlStr = NOU_URL + "CTBC/CTBC.go";
				InputStream is = null;
				
				JSONObject jb2 = new JSONObject();
				
				jb2.put("op", "Q0002");
				jb2.put("rid", rid);
				
				String jsonStr = String.valueOf(jb2);
				
				logger.debug("queryJson = " + jsonStr);
	    		
				url = new URL(urlStr);
				
				logger.debug("query Url = " + urlStr);
				System.out.println("query Url = " + urlStr);
				
				try{
					SSLUtil.ignoreSsl();
					connection = (HttpsURLConnection)url.openConnection();
				} catch(ClassCastException cse){
					connection = (HttpURLConnection)url.openConnection();
				}
				
				connection.setRequestMethod("POST");
				
				//必須設置false，否則會自動redirect到重定向後的地址
				connection.setInstanceFollowRedirects(false);
				//設置允許對外輸出數據
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Length", (""+jsonStr.getBytes().length));
				connection.setRequestProperty("Content-Type", "application/json");
				//提交到服務器
				connection.getOutputStream().write(jsonStr.getBytes());
				connection.setConnectTimeout(5000);
				
				JSONObject jsonObject = null;
                String stuName = "";
                String amt = "";
                String payduedate = "";
                
                logger.debug("getResponseCode = " + connection.getResponseCode());
                
				if (connection.getResponseCode() == 200) {
	                is = connection.getInputStream();
	                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
	                
	                String returnValue = br.readLine();
	                br.close();
	                
	                logger.debug("Q0002 return value = " + returnValue);
	                //System.out.println("return value = " + returnValue);
	                
	                jsonObject = new JSONObject(returnValue);
	                jsonObject = new JSONObject(jsonObject.getString("result"));
	                
	                if("failed".equals(jsonObject.getString("status"))) {
	                	//失敗
	                	errcode = jsonObject.getString("errcode");
	                	logger.debug("Q0002 return errcode = " + errcode);
	                } else {	                	
		                stuName = jsonObject.getString("NAME");
		                amt = jsonObject.getString("PAYABLE_TOTAL_AMT");
		                if("".equals(jsonObject.getString("STNO"))) {
		                	payduedate = jsonObject.get("OLD_EDATE_ATM").toString();
		                } else {
		                	payduedate = jsonObject.get("NEW_EDATE_ATM").toString();
		                }
		                
//		                System.out.println("name = " + stuName);
//		                System.out.println("amt = " + amt);
//		                System.out.println("stno = " + jsonObject.getString("STNO"));
		                logger.debug("Q0002 return stuName = " + stuName);
		                logger.debug("Q0002 return amt = " + amt);
		                logger.debug("Q0002 return stno = " + jsonObject.getString("STNO"));
	                }
	            }
	    		
				jsonObject = new JSONObject();
				
                jsonObject.put("op", "Q0002");
                jsonObject.put("errcode", errcode);
                
                if("5292".equals(rid.substring(0, 4))) {
                    jsonObject.put("schoolname", SCHOOL_NAME_2);
                } else {
                	jsonObject.put("schoolname", SCHOOL_NAME);
                }
                              
                //jsonObject.put("studentname", new String( stuName.getBytes("utf-8") , "utf-8"));
                jsonObject.put("studentname", stuName);
                jsonObject.put("amount", amt);
                jsonObject.put("payduedate", payduedate);
                
                
                Iterator it = jsonObject.keys();
    			StringBuffer sb = new StringBuffer();
    			while(it.hasNext()){
    				String key = (String) it.next();
    				String value = jsonObject.optString(key);
    				if("studentname".equals(key)) {
    					try {
							value = new String( value.getBytes("utf-8") , "utf-8");
						} catch (UnsupportedEncodingException e) {
							logger.debug("value轉UTF8發生錯誤喔");
							e.printStackTrace();
						}
    				}
    				sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
    			}
                
                result = "<?xml version=\"1.0\" encoding=\"utf-8\"?><NOU>" +  sb.toString() + "</NOU>";
                System.out.println("result = " + result);
                
				return result;
	        	
	    	} catch(Exception ex) {
	    		ex.printStackTrace();
	    		logger.debug(ex.toString());
	    		
    			JSONObject jsonObject = new JSONObject();
                
    			if("0000".equals(errcode)) {
    				errcode = "xxxx";
    			}
    			
    			
                try {
					jsonObject.put("op", "Q0002");
					jsonObject.put("errcode", errcode);
	                jsonObject.put("schoolname", SCHOOL_NAME);
	                jsonObject.put("studentname", "");
	                jsonObject.put("amount", "");
	                jsonObject.put("payduedate", "");
	                
	                Iterator it = jsonObject.keys();
	    			StringBuffer sb = new StringBuffer();
	    			while(it.hasNext()){
	    				String key = (String) it.next();
	    				String value = jsonObject.optString(key);
	    				if("studentname".equals(key)) {
	    					try {
								value = new String( value.getBytes("utf-8") , "utf-8");
							} catch (UnsupportedEncodingException e) {
								logger.debug("value轉UTF8發生錯誤喔");
								e.printStackTrace();
							}
	    				}
	    				sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
	    			}

	    			logger.debug("errcode = " + CTBC_errcode.getCTBCErrorMsg(errcode));
	    			return "<NOU>" +  sb.toString() + "</NOU>";
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		 
	    	}
	    	//logger.debug("errcode 轉換 = " + CTBC_errcode.getCTBCErrorMsg(errcode));
	    	logger.debug("errcode = " + errcode);
	    	return result;
	    }
	    /**						
	     * @param rid
	     * @return
	     * 
	     * 接收中信傳過來的繳款成功資訊
	     **/
		public String T0001(String QueryString) {
			String result = "";
			Logger logger  =  Logger.getLogger("T0001 Start");
			
			String NOU_URL = "https://noustud.nou.edu.tw/";
	    	
	    	try{
	    		NOU_URL = MyProperties.getProperties("NOU_URL");
	    	} catch(Exception e) {
	    		logger.debug("取得參數'NOU_URL'時發生錯誤");
	    		e.printStackTrace();
	    	}
			
			try {
				String errcode = "0000";
				String MAC = "";
				//PropertyConfigurator.configure("D:/Workspace/JAVA/Nou/NouRelayStation_web/axis2/WebContent/WEB-INF/classes/log4j.properties");
	            
	            logger.debug("T0001 QueryString1 = " + QueryString);
				
				CTBCPaymentNotice ctbcPnDAO = new CTBCPaymentNotice();
				
				logger.debug("T0001 QueryString1 = " + QueryString);
				
				try {
					if("".equals(QueryString) || QueryString == null) {
		        		errcode = "S999";
		        	} 
					
					JSONObject jb = new JSONObject(QueryString);
					
					try{
	            		ctbcPnDAO.setOp(jb.get("op").toString());
	    				ctbcPnDAO.setOrgid(jb.get("orgid").toString());
	    				ctbcPnDAO.setPwd(jb.get("pwd").toString());
	    				ctbcPnDAO.setSeqno(jb.get("seqno").toString());
	    				ctbcPnDAO.setRid(jb.get("rid").toString());
	    				ctbcPnDAO.setAmount(jb.get("amount").toString());
	    				ctbcPnDAO.setPaydate(jb.get("paydate").toString());
	    				ctbcPnDAO.setPaytime(jb.get("paytime").toString());
	    				ctbcPnDAO.setAuthcode(jb.get("authcode").toString());
	    				ctbcPnDAO.setMAC(jb.get("MAC").toString());
	            	} catch(Exception ex) {
	            		logger.debug("T0001 DAO set error");
	            		errcode = "S999";
	            	}
					
					MAC = jb.get("MAC").toString();
					
					//繳費成功要回寫道post012 丟回去外網做
		    		String urlStr = "";
					urlStr = NOU_URL + "CTBC/CTBCPaySuccess";
					InputStream is = null;
					
					JSONObject jb2 = new JSONObject();
					
					jb2.put("op", "T0001");
					jb2.put("rid", ctbcPnDAO.getRid());
					jb2.put("paytime", ctbcPnDAO.getPaytime());
					jb2.put("paydate", ctbcPnDAO.getPaydate());
					jb2.put("authcode", ctbcPnDAO.getAuthcode());				
					
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
					Date date = new Date();
					
					jb2.put("srvDate", sdf.format(date));
					
					
					String jsonStr = String.valueOf(jb2);
					
					logger.debug("Query jsonStr = " + jsonStr);
		    		
					url = new URL(urlStr);
					
					try{
						SSLUtil.ignoreSsl();
						connection = (HttpsURLConnection)url.openConnection();
					} catch(ClassCastException cse){
						connection = (HttpURLConnection)url.openConnection();
					}
					
					connection.setRequestMethod("POST");
					
					//必須設置false，否則會自動redirect到重定向後的地址
					connection.setInstanceFollowRedirects(false);
					//設置允許對外輸出數據
					connection.setDoInput(true);
					connection.setDoOutput(true);
					connection.setRequestProperty("Content-Length", (""+jsonStr.getBytes().length));
					connection.setRequestProperty("Content-Type", "application/json");
					//提交到服務器
					connection.getOutputStream().write(jsonStr.getBytes());
					connection.setConnectTimeout(5000);
					
					if(connection.getResponseCode() == 200) {
						logger.debug("connection = 200");
						jb = new JSONObject();
						jb.put("op", "T0001");
						jb.put("errcode", errcode);
						jb.put("MAC", MAC);
						
						Iterator it = jb.keys();
		    			StringBuffer sb = new StringBuffer();
		    			while(it.hasNext()){
		    				String key = (String) it.next();
		    				String value = jb.optString(key);
		    				sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
		    			}
		    			
		    			result = "<NOU>" +  sb.toString() + "</NOU>";
		    			logger.debug("T0001 result = " + result);
		    			return result;
					} else {
						logger.debug("connection error");
						throw new Exception("無法連到" + NOU_URL);
					}
					
				} catch (Exception e) {
					logger.debug("in exception : " + e.toString());
					if("0000".equals(errcode)) {
	    				errcode = "xxxx";
	    			}
					
					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("op", "T0001");
						jsonObject.put("errcode", errcode);
		                jsonObject.put("MAC", MAC);
		                
		                Iterator it = jsonObject.keys();
		    			StringBuffer sb = new StringBuffer();
		    			while(it.hasNext()){
		    				String key = (String) it.next();
		    				String value = jsonObject.optString(key);
		    				sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
		    			}

		    			logger.debug("errcode 轉換 = " + CTBC_errcode.getCTBCErrorMsg(errcode));
		    			result = "<NOU>" +  sb.toString() + "</NOU>";
		    			return result;
					} catch (JSONException je) {
						// TODO Auto-generated catch block
						je.printStackTrace();
					}
					
					e.printStackTrace();
				}
			} catch (Exception e){
				logger.debug("系統發生錯誤: " + e.toString());
				e.printStackTrace();
			}
			
			return result;
		}

}
