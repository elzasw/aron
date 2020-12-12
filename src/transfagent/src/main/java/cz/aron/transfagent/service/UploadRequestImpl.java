package cz.aron.transfagent.service;

import com.lightcomp.ft.client.AbstractRequest;
import com.lightcomp.ft.client.UploadRequest;
import com.lightcomp.ft.core.send.items.SourceItemReader;
import com.lightcomp.ft.xsd.v1.GenericDataType;

public class UploadRequestImpl extends AbstractRequest implements UploadRequest {

	private final SourceItemReader sourceItemReader;
	
    boolean canceled = false;
    
    boolean failed = false;
    
    GenericDataType response;

	protected UploadRequestImpl(GenericDataType data, SourceItemReader sourceItemReader) {
		super(data);
		this.sourceItemReader = sourceItemReader;
	}

	@Override
	public SourceItemReader getRootItemsReader() {
		return sourceItemReader;
	}			

	@Override
	public void onTransferSuccess(GenericDataType response) {		
		super.onTransferSuccess(response);
		this.response = response;
	}

	@Override
	public void onTransferCanceled() {
		super.onTransferCanceled();
		this.canceled = true;
	}

	@Override
	public void onTransferFailed() {
		super.onTransferFailed();
		this.failed = true;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	public GenericDataType getResponse() {
		return response;
	}

	public void setResponse(GenericDataType response) {
		this.response = response;
	}

	public static UploadRequestImpl buildRequest(SourceItemReader sourceItemReader, String id) {
		GenericDataType dataType = new GenericDataType();
		dataType.setType("APUSRC");
		dataType.setId(id);
		return new UploadRequestImpl(dataType, sourceItemReader);
	}

}
