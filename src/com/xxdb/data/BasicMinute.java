package com.xxdb.data;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

import com.xxdb.io.ExtendedDataInput;

/**
 * 
 * Corresponds to DolphinDB minute scalar
 *
 */

public class BasicMinute extends BasicInt{
	private static DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm'm'");

	public BasicMinute(LocalTime value){
		super(Utils.countMinutes(value));
	}
	
	public BasicMinute(ExtendedDataInput in) throws IOException {
		super(in);
	}
	
	protected BasicMinute(int value){
		super(value);
	}
	
	@Override
	public DATA_CATEGORY getDataCategory() {
		return Entity.DATA_CATEGORY.TEMPORAL;
	}

	@Override
	public DATA_TYPE getDataType() {
		return Entity.DATA_TYPE.DT_MINUTE;
	}
	
	public LocalTime getMinute(){
		if(isNull())
			return null;
		else
			return Utils.parseMinute(getInt());
	}
	
	@Override
	public Temporal getTemporal() throws Exception {
		return getMinute();
	}
	
	@Override
	public String getString() {
		if(isNull())
			return "";
		else
			return getMinute().format(format);
	}
	
	@Override
	public boolean equals(Object o){
		if(! (o instanceof BasicMinute) || o == null)
			return false;
		else
			return getInt() == ((BasicMinute)o).getInt();
	}
}
