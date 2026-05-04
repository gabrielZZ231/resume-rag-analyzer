package org.acme.analysis.adapter.in.dto;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.util.List;

public class FileUploadForm {

    @RestForm("file")
    public List<FileUpload> files;

    @RestForm("jobDescription")
    public String jobDescription;

    @RestForm("company")
    public String company;

    @RestForm("role")
    public String role;
}
