package uk.co.stikman.invmon.datalog;

import uk.co.stikman.invmon.InvMonException;

/**
 * 
 * @author stik
 *
 */
public interface Table {

	/**
	 * key should be boxed types
	 * 
	 * @param key
	 * @param field
	 * @return
	 * @throws InvMonException 
	 */
	float getFloat(Object[] key, String field) throws InvMonException;

	void setFloat(Object[] key, String field, float val) throws InvMonException;

}
