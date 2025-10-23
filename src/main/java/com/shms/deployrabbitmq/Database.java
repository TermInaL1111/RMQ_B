package com.shms.deployrabbitmq;

import java.io.*;
import java.nio.file.Files;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.*;


//完全复用 实现 用户的账号密码 存储 读取
public class Database {


    private String filePath;


    public Database(String filePath){
        this.filePath = filePath;
        initFile(); // 自动初始化文件
    }
    private void initFile() {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        if (!file.exists()) {
            try {
                Files.writeString(file.toPath(), "<data></data>");
                System.out.println("已创建用户数据库文件：" + file.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean userExists(String username){

        try{
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("user");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if(getTagValue("username", eElement).equals(username)){
                        return true;
                    }
                }
            }
            return false;
        }
        catch(Exception ex){
            System.out.println("Database exception : userExists()");
            return false;
        }
    }

    public boolean checkLogin(String username, String password){

        if(!userExists(username)){ return false; }

        try{
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("user");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if(getTagValue("username", eElement).equals(username) && getTagValue("password", eElement).equals(password)){
                        return true;
                    }
                }
            }
            System.out.println("Hippie");
            return false;
        }
        catch(Exception ex){
            System.out.println("Database exception : userExists()");
            return false;
        }
    }

    public void addUser(String username, String password){

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filePath);

            Node data = doc.getFirstChild();

            Element newuser = doc.createElement("user");
            Element newusername = doc.createElement("username"); newusername.setTextContent(username);
            Element newpassword = doc.createElement("password"); newpassword.setTextContent(password);

            newuser.appendChild(newusername); newuser.appendChild(newpassword); data.appendChild(newuser);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);

        }
        catch(Exception ex){
            System.out.println("Exceptionmodify xml");
        }
    }

    public static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        return nValue.getNodeValue();
    }
}
