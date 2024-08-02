package dao;

import java.util.ArrayList;
import org.hibernate.Session;

import models.Blog;
import models.BreachedPassword;
import utils.HibernateUtil;
import utils.LoggerUtils;


public class BreachedPasswordRepository {
    
    public ArrayList<String> findEncryptedPasswords(){
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()){
            return(ArrayList<String>) session
            		.createQuery("SELECT encryptedPassword FROM BreachedPassword", String.class)
            		.getResultList();
        } catch (Exception e) {
            LoggerUtils.log("findEncryptedPasswords: Error : " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<String>();
    }
    
    
public ArrayList<BreachedPassword> findUnencryptedPasswords(){
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()){
            return(ArrayList<BreachedPassword>) session
        			.createQuery("from BreachedPassword", BreachedPassword.class)
            		.getResultList();
        } catch (Exception e) {
            LoggerUtils.log("findUnencryptedPasswords: Error : " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<BreachedPassword>();
    }
    
}
