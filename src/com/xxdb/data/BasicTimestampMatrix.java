package com.xxdb.data;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import com.xxdb.io.ExtendedDataInput;
/**
 * 
 * Corresponds to DolphinDB timestamp matrix
 *
 */

public class BasicTimestampMatrix extends BasicLongMatrix{
	public BasicTimestampMatrix(int rows, int columns){
		super(rows, columns);
	}
	
	public BasicTimestampMatrix(int rows, int columns, List<long[]> listOfArrays) throws Exception {
		super(rows,columns, listOfArrays);
	}
	
	public BasicTimestampMatrix(ExtendedDataInput in) throws IOException {
		super(in);
	}

	public void setTimestamp(int row, int column, LocalDateTime value){
		setLong(row, column, Utils.countMilliseconds(value));
	}
	
	public LocalDateTime getTimestamp(int row, int column){
		return Utils.parseTimestamp(getLong(row, column));
	}
	

	@Override
	public Scalar get(int row, int column) {
		return new BasicTimestamp(getLong(row, column));
	}

	@Override
	public DATA_CATEGORY getDataCategory() {
		return DATA_CATEGORY.TEMPORAL;
	}

	@Override
	public DATA_TYPE getDataType() {
		return DATA_TYPE.DT_TIMESTAMP;
	}
	
	@Override
	public Class<?> getElementClass(){
		return BasicTimestamp.class;
	}

}
