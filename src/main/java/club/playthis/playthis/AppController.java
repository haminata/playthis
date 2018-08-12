package club.playthis.playthis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class AppController {

    @GetMapping("/schema")
    public String getSchema(){
        return DbModel.schemas().toFormattedString();
    }
}
