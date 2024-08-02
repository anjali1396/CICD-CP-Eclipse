package dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Session;

public class GenericRepository {

	public static <T> List<T> listAll(Class<T> type, Session session) {
		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<T> criteria = builder.createQuery(type);
		criteria.from(type);
		List<T> data = session.createQuery(criteria).getResultList();
		return data;
	}

}
