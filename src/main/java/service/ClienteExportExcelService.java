package service;

import dto.resp.ClienteResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.ClienteRepository;
import java.util.List;

@Service
public class ClienteExportExcelService {
    @Autowired
    private ClienteRepository clienteRepo;
    @Autowired
    private ClienteService clienteService;
    public byte[] exportarExcel(
            Boolean soloDeudores,
            String nombre
    ) {
        List<ClienteResp> clientes = clienteService.findPaged(
                0,
                Integer.MAX_VALUE,
                soloDeudores,
                nombre
        ).getContent();

        return excelUtil.generar(clientes);
    }



}
