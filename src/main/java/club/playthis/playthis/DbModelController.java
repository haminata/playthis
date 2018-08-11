package club.playthis.playthis;

import com.mysql.cj.xdevapi.DbDoc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/db")
public class DbModelController {

    @GetMapping("/{modelName}")
    public String findByTitle(@PathVariable String modelName) {
        System.out.println("[plural model] " + modelName);
        Class<? extends DbModel> cls = DbModel.getModelPluralNames().get(modelName);
        if(cls != null) return Utils.responseJson(DbModel.all(cls)).toFormattedString();

        throw new RuntimeException("id-" + modelName);
    }

}