package com.esign.controller;

import com.esign.util.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import com.esign.configuration.Configs;
import com.esign.model.DocumentEntity;
import com.esign.model.EventDocument;
import com.esign.model.SignatureRequest;
import com.esign.model.SignatureResponse;
import com.esign.model.TokenInfoEntity;
import com.esign.model.AgreementAdobe.*;
import com.esign.model.AgreementAdobe.Responses.AdobeListEvents;
import com.esign.model.AgreementRequest.*;
import com.esign.repository.DocumentRepository;
import com.esign.repository.TokenInfoRepository;
import com.esign.services.FilesStorageService;
import com.esign.util.AESEncryptionDecryption;
import com.esign.util.SendAgreement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;


@RestController
@CrossOrigin
@Component
@RequestMapping("/api/v1")
public class DocumentServiceController {

    @Autowired
    private Configs configs;
    @Autowired
    FilesStorageService storageService;
    @Autowired
    TokenInfoRepository tokenInfoRepository;
    @Autowired
    DocumentRepository documentRepository;

    @RequestMapping(path = "/coletar-assinaturas", method = RequestMethod.POST)
    public SignatureResponse colectSignature(@RequestBody SignatureRequest nRequest) throws IOException, ParseException 
    {   
        TokenInfoEntity configApi = Tools.refreshTokenAccess(tokenInfoRepository);
       AgreementRequest agreementRequest = new AgreementRequest(nRequest.getTitulo(), "ESIGN", "IN_PROCESS", nRequest.getLibraryDocumentId());
      
        agreementRequest.setInformacaoParticipantes(nRequest.getAssinantes());
        agreementRequest.setInformacaoCampos(nRequest.getCampos());
        agreementRequest.setInformacaoCc(nRequest.getCc());
        
            SendAgreement sendAgreement = new SendAgreement(configApi.getToken_string(), agreementRequest);
           
            sendAgreement.init(nRequest.getLibraryDocumentId());
            
            String response = sendAgreement.getLibraryDocumentId();
                DocumentEntity documentEntity = new DocumentEntity(nRequest.getReferencia(), response, sendAgreement.getId(),nRequest.getProcesso(), nRequest.getTitulo());
                documentRepository.save(documentEntity);
            //}
            SignatureResponse aSignatureResponse = new SignatureResponse(sendAgreement.getId(),sendAgreement.getStatusCode(),sendAgreement.getStatus().getReasonPhrase());
            
            return aSignatureResponse;
    }

    
    
    @RequestMapping(path = "/acordo/download/{reference}", method = RequestMethod.GET)
    public void downloadAgreement(@PathVariable("reference") String reference)throws FileNotFoundException, ParseException 
     {
        TokenInfoEntity configApi = Tools.refreshTokenAccess(tokenInfoRepository);
        SendAgreement agreement = new SendAgreement(configApi.getToken_string());
        agreement.setUploadsPath(configs.getUploads());
          agreement.download(reference, configs.getUploads()) ;
    }
    
    /*
    // @GetMapping("documentos/acordo/preview/{reference}")
    @RequestMapping(path = "/acordo/preview/{reference}", method = RequestMethod.GET, produces = {
            MediaType.APPLICATION_PDF_VALUE })
    public ResponseEntity<InputStreamResource> previewAgreement(@PathVariable String reference)throws FileNotFoundException, ParseException 
     {
        String folderPath = configs.getUploads() + "signed/";
        HttpHeaders headers = new HttpHeaders();
        TokenInfoEntity configApi = Tools.refreshTokenAccess(tokenInfoRepository);
        SendAgreement agreement = new SendAgreement(configApi.getToken_string());
        agreement.setUploadsPath(configs.getUploads());
        String filename = reference + ".pdf";
        File file = new File(folderPath + filename);
       
        // METODO A ELIMINAR NO NOVO
        
        if (file.exists())
        {
            file.delete();
            agreement.downloadFileFromPath(reference);  
        }
        else 
        {
            agreement.downloadFileFromPath(reference); 
        }
        
        
     // FINAL DO METODO A ELIMINAR NO NOVO
       
       
        headers.add("content-disposition", "inline;filename=" + filename);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok().headers(headers).contentLength(file.length())
        
                .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
    }
    */
    
  
    @RequestMapping(path = "/acordo/evento/{reference}", method = RequestMethod.GET, produces = {
            MediaType.APPLICATION_JSON_VALUE })
    public AdobeListEvents showEvents(@PathVariable String reference) throws ParseException 
    {
        DocumentEntity document = documentRepository.findByReference(reference);
        TokenInfoEntity configApi = Tools.refreshTokenAccess(tokenInfoRepository);
        SendAgreement agreement = new SendAgreement(configApi.getToken_string());
        AdobeListEvents eventsResponse = agreement.getDocumentEvents(document.getTransactionId());
        return eventsResponse;
    }

    @RequestMapping(path = "/acordo/evento/{reference}/lastevent", method = RequestMethod.GET, produces = {
            MediaType.APPLICATION_JSON_VALUE })
    public EventDocument getLastEvent(@PathVariable String reference) throws ParseException 
    {
        DocumentEntity document = documentRepository.findByReference(reference);
        TokenInfoEntity configApi = Tools.refreshTokenAccess(tokenInfoRepository);
        SendAgreement agreement = new SendAgreement(configApi.getToken_string());
        AdobeListEvents eventsResponse = agreement.getDocumentEvents(document.getTransactionId());
        int lastIndex = eventsResponse.getEvents().size();
        EventDocument event = new EventDocument(eventsResponse.getEvents().get(lastIndex - 1));
        return event;
    }

    @RequestMapping(path = "/teste", method = RequestMethod.POST)
    public String teste(@ModelAttribute SignatureRequest agreement) throws IOException
    {
        Optional<TokenInfoEntity> configs = tokenInfoRepository.findById((long) 1);
        TokenInfoEntity configApi = configs.get();
        Date currentDate = new Date();
        if (configApi.getExpireted_at().compareTo(currentDate) <= 0) 
        {
            AdobeRefreshToken refreshToken = new AdobeRefreshToken(configApi.getRefresh_token(),
                    configApi.getClient_id(), configApi.getClient_secret());
            ResponseRefreshToken responseRefreshToken = refreshToken.refreshOldToken();
            Calendar calendar = Calendar.getInstance();
            Date today = calendar.getTime();
            calendar.setTimeInMillis(responseRefreshToken.getExpires_in());
            Calendar timeout = Calendar.getInstance();
            timeout.setTimeInMillis(today.getTime() + responseRefreshToken.getExpires_in() * 1000);
            configApi.setExpireted_at(timeout.getTime());
            configApi.setUpdated_at(today);
            configApi.setToken_string(responseRefreshToken.getToken_type() + " " + responseRefreshToken.getAccess_token());
            tokenInfoRepository.save(configApi);
        }
        RestTemplate res = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "*/*");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", configApi.getToken_string());
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
        ResponseEntity<String> response = res.exchange( this.configs.getApibaseurl()+"/transientDocuments",HttpMethod.POST, requestEntity,String.class);
        
        return response.getBody();
    }

    @RequestMapping(path = "/testando", method = RequestMethod.GET)
    public SignatureRequest testandoRequest(@RequestBody SignatureRequest nRequest) 
    {
        return nRequest;
    }
    
    @RequestMapping(path = "/acordo/preview/{reference}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_PDF_VALUE })
   // public ResponseEntity<Resource> download() throws IOException {
    public ResponseEntity<Resource> download(@PathVariable String reference) throws IOException {
        
        String folderPath = configs.getUploads(); 
        
        String documentName = reference + ".pdf";
        File file = new File(folderPath + "signed/"+ documentName);
        if (file.exists()){
            HttpHeaders header = new HttpHeaders();
            header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+ documentName );
            header.add("Cache-Control", "no-cache, no-store, must-revalidate");
            header.add("Pragma", "no-cache");
            header.add("Expires", "0");
            Path path = Paths.get(file.getAbsolutePath());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            return ResponseEntity.ok().headers(header).contentLength(file.length()).contentType(MediaType.parseMediaType("application/octet-stream")).body(resource);
        }
        else {
            File file2 = new File(folderPath + "unsigned/" + documentName);
            if (file2.exists()){
            HttpHeaders header = new HttpHeaders();
            header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+ documentName);
            header.add("Cache-Control", "no-cache, no-store, must-revalidate");
            header.add("Pragma", "no-cache");
            header.add("Expires", "0");
            Path path = Paths.get(file2.getAbsolutePath());  
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            return ResponseEntity.ok().headers(header).contentLength(file2.length()).contentType(MediaType.parseMediaType("application/octet-stream")).body(resource);
    }
            else { System.out.println("File does not exists on the server"); return null;}
        } 
    }  
    

}
