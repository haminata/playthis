package club.playthis.playthis.client;

import club.playthis.playthis.Utils;
import club.playthis.playthis.db.DbModel;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/db")
public class DbModelController {

    @GetMapping("/{modelName}")
    public String getAll(@PathVariable String modelName) {
        System.out.println("[plural model] " + modelName);
        Class<? extends DbModel> cls = DbModel.getModelPluralNames().get(modelName);
        if(cls != null) return Utils.responseJson(DbModel.all(cls)).toFormattedString();

        throw new RuntimeException("id-" + modelName);
    }

    @PostMapping("/{modelName}")
    public String saveModel(@PathVariable String modelName){

        return "{\"\"}";
    }

}