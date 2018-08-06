/**
 * Created by haminata on 04/08/2018.
 */

const e = React.createElement;

const VIEW_FORMAT = {
    LIST_ITEM: 'list_item',
    GRID_ITEM: 'grid_item',
    DEFAULT: 'default'
};

class DbModel extends React.Component {

    constructor(props){
        super(props || {});
        this.state = {viewFormat: VIEW_FORMAT.DEFUALT};
        //this.constructor.cache.push(this);
    }

    set viewFormat(format){
        this.setState({viewFormat: format});
    }

    get viewFormat(){
        return this.state.viewFormat || VIEW_FORMAT.DEFAULT;
    }

    render(){
        return e('h1', {ref: window.textInput}, 'Hello World')
    }

    static get cache(){
        if(this.name === DbModel.name){
            return this.CACHE;
        } else {
            let clsName = this.name;
            let models = DbModel.CACHE[clsName] = DbModel.CACHE[clsName] || [];
            return models;
        }
    }

    static get schema(){
        return DbModel.SCHEMAS[this.name]
    }

    schema(){
        return this.constructor.schema;
    }

    static get namePlural(){
        return this.schema.plural;
    }

    get namePlural(){
        return this.constructor.namePlural;
    }

    static all(){
        return app.getJson(`${this.namePlural}`)
            .then((resp) => resp.data)
    }

    all(){
        return this.constructor.all();
    }

    static getModelClass(modelName){
        let cls = _.attempt(eval, modelName);
        if(cls && !_.isError(cls)) return cls;
        else return null;
    }

    static setSchema(modelName, schema){
        this.SCHEMAS[modelName] = schema;


        let cls = this.getModelClass(modelName);

        if(!cls) return;

        _.each(schema.properties, (s, propName) => {
            let desc = Object.getOwnPropertyDescriptor(cls.prototype, propName);

            if(!desc || !desc.get){
                Object.defineProperty(cls.prototype, propName, {
                    get: function () {
                        return this.state[propName] || this.props[propName];
                    },
                    set: function (value) {
                        this.setState({propName: value});
                    }
                })
            }
        })
    }
}

DbModel.SCHEMAS = {};
DbModel.CACHE = {};

class Musicroom extends DbModel {

    render(){
        const style = {
            backgroundColor : this.name,
        };

        return e('h4', {style}, 'Hello World')
    }
}

class ModelCollection extends React.Component {

    render(){
        let elems = _.map(this.props.modelsProps || [], (prop) => {
            let key = `${this.props.model.name}_${prop.id}`;
            let el = e(this.props.model, _.merge({key}, prop));

            return e('li', {key}, [el]);
        });

        return e('ul', {}, elems);
    }

}

$(() => {
    ReactDOM.render(e(Musicroom, {name: 'blue'}), document.getElementsByClassName('container')[0], function(){
        console.log('[react] rendered:', arguments);
    })
});