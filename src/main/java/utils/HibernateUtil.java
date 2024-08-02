package utils;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import models.AdBannerImage;
import models.Address;
import models.Blog;
import models.BreachedPassword;
import models.Creds;
import models.DeleteAccountRequest;
import models.LendingProducts;
import models.LoginInfo;
import models.OTPManager;
import models.PromoBanners;
import models.ROIReprice;
import models.Referrals;
import models.RequestOutstanding;
import models.SecondaryInfo;
import models.ServiceRequest;
import models.User;
import models.UserRequest;
import models.notification.CPNotification;
import models.notification.UserNotificationInfo;
import models.notification.UserNotificationToken;
import models.payment.Payment;
import models.sob.Promo;
import models.sob.Prospect;
import utils.ProptertyUtils.Keys;

public class HibernateUtil {

	private static SessionFactory sessionFactory = null;
	
	public static SessionFactory getSessionFactory() throws Exception {
		
		if (sessionFactory == null) {
		
			Configuration config = new Configuration();
			
			Properties settings = new Properties();			
			settings.put(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
			
			if (Constants.IS_DB_LIVE) {
								
				settings.put(Environment.URL, "jdbc:mysql://hffccustomerportal.c5jfvga5d4bj.ap-south-1.rds.amazonaws.com:3306/HomeFirstCustomerPortal?useUnicode=true&autoReconnect=true");
				settings.put(Environment.USER, "masternaol_hffc");
				settings.put(Environment.PASS, ProptertyUtils.getValurForKey(Keys.PROD_DB_PASS));
				
			} else {
				
				settings.put(Environment.USER, "root");
				
//				if (Constants.IS_STAGING) {
//					settings.put(Environment.USER, "naol");
//					settings.put(Environment.URL, "jdbc:mysql://65.2.48.131:3306/HomeFirstCustomerPortal?useUnicode=true&autoReconnect=true");
//					settings.put(Environment.PASS, "$Hffc9_SarikaML");
//					
//				}
				if (Constants.IS_STAGING) {
					settings.put(Environment.USER, "write_user");
					settings.put(Environment.URL, "jdbc:mysql://mobility-staging-db.cjminpnxnvgv.ap-south-1.rds.amazonaws.com:3306/HomeFirstCustomerPortal?createDatabaseIfNotExist=true");
					settings.put(Environment.PASS, "Px7G0Dqx99LignQsGs");
					
				}
				
				else {
					
					// p@radise7_mysql - rabit
					// Ranan#123 - Ranan	
					// Anjali@123 - Anjali
					// Hffc@123 - Shubham
					// sid@123 - Siddhant
				
					settings.put(Environment.URL, "jdbc:mysql://localhost:3306/HomeFirstCustomerPortal?useUnicode=true&autoReconnect=true");
					settings.put(Environment.USER, "root");
					settings.put(Environment.PASS, "Hffc@123"); // TODO: Change here for your local
					
				}							
				
			}
			
			settings.put(Environment.DIALECT, "org.hibernate.dialect.MySQL57Dialect");
			settings.put(Environment.SHOW_SQL, "false");
			settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
			settings.put(Environment.HBM2DDL_AUTO, "update");
			settings.put("hibernate.id.new_generator_mappings","false");
			settings.put("hibernate.jdbc.batch_size", Constants.HITBERNATE_BATCH_SIZE);
			
			config.setProperties(settings);
			config.addAnnotatedClass(Prospect.class);
			config.addAnnotatedClass(Address.class);
			config.addAnnotatedClass(Promo.class);
			config.addAnnotatedClass(Blog.class);	
			config.addAnnotatedClass(LendingProducts.class);	
			config.addAnnotatedClass(Payment.class);
			config.addAnnotatedClass(User.class);
			config.addAnnotatedClass(LoginInfo.class);
			config.addAnnotatedClass(SecondaryInfo.class);
			config.addAnnotatedClass(UserRequest.class);
			config.addAnnotatedClass(CPNotification.class);
			config.addAnnotatedClass(UserNotificationInfo.class);
			config.addAnnotatedClass(ROIReprice.class);
			config.addAnnotatedClass(ServiceRequest.class);
			config.addAnnotatedClass(RequestOutstanding.class);
			config.addAnnotatedClass(PromoBanners.class);
			config.addAnnotatedClass(AdBannerImage.class);
			config.addAnnotatedClass(DeleteAccountRequest.class);
			config.addAnnotatedClass(Creds.class);
			config.addAnnotatedClass(UserNotificationToken.class);			
			config.addAnnotatedClass(Referrals.class);	
			config.addAnnotatedClass(BreachedPassword.class);

			
			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
			
			sessionFactory = config.buildSessionFactory(serviceRegistry);
			
		}
		
		return sessionFactory;
		
	}
	
}
