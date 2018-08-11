/**
 * Created by haminata on 04/08/2018.
 */

const e = React.createElement;

Object.assign(e, ReactDOMFactories);

const VIEW_FORMAT = {
    LIST_ITEM: 'list_item',
    GRID_ITEM: 'grid_item',
    FULL: 'full'
};

class DbModel extends React.Component {

    constructor(props){
        super(props || {});
        this.state = Object.assign({viewFormat: VIEW_FORMAT.FULL}, this.props);
        //this.constructor.cache.push(this);
    }

    set viewFormat(format){
        this.setState({viewFormat: format});
    }

    get viewFormat(){
        return this.state.viewFormat || VIEW_FORMAT.DEFAULT;
    }

    get style(){
        return {}
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

    get createdBy(){
        return _.isObject(this.state.createdBy) ? this.state.createdBy : {name: 'Anonymous'}
    }

    render(){
        const style = {
            backgroundColor : this.name,
            maxHeight: '20vh',
            marginTop: '16px'
        };
        let key = `${this.constructor.name}_${this.state.id}`;

        let onJoinClick = () => {
          app.setState({selectedRoom: this.state});
        };

        return e.div({style, key, className: 'card'}, [
            //e.img({className: 'card-img-top', alt: 'Card image cap', src: ".../100px180/"}),
            //e.div({className: 'card-header'}, this.props.name || 'No header'),
            e.div({className: 'card-body'}, [
                e.h5({className: 'card-title', key}, this.state.name || 'No title'),
                e.h6({className: 'card-subtitle mb-2 text-muted'}, `By ${this.createdBy.name}`),
                e.p({className: 'card-text'}, this.state.description || 'No description'),
                e.a({className: 'card-link', href: '#', onClick: onJoinClick}, 'Join'),
            ])
        ])
    }
}

class ModelCollection extends React.Component {

    render(){
        let elems = _.map(this.props.modelsProps || [], (prop) => {
            let key = `${this.props.model.name}_${prop.id}`;
            let el = e(this.props.model, _.merge({key, viewFormat: VIEW_FORMAT.GRID_ITEM}, prop));

            let style = {};
            return e('div', {key, className: 'col-md-4 col-lg-4 col-sm-6', style}, [el]);
        });

        return e.div({className: 'container'}, e.div({className: 'row'}, elems));
    }

}

class NavBar extends React.Component {


    render(){
        let children = [
            e.a({className: 'navbar-brand', href: '#'}, app.name),
            e.button({className: 'navbar-toggler', type: "button", 'data-toggle': "collapse", 'data-target': "#navbarText"}, [
                e.span({className: 'navbar-toggler-icon'})
            ])
        ];
        return e.nav({className: 'nav navbar navbar-dark navbar-expand-sm bg-dark'}, children)
    }
}

class AppView extends React.Component {

    constructor(props){
        super(props || {});
        this.state = {
            user: null,
            musicrooms: [],
            newStack: [],
            selectedRoom: null
        }
    }

    get name(){
        return 'PlayThis'
    }

    getSchemas(){
        if(_.isPlainObject(this._modelSchemas)) Promise.resolve(this._modelSchemas);

        return this.getJson('/schemas')
            .then((res) => {
                console.log('[getSchemas]', res);

                _.each(res, (schema, modelName) => {
                    this.models[modelName] = DbModel.setSchema(modelName, schema);
                })
            })
    }

    getJson(url) {
        return new Promise((resolve, reject) => {
            $.ajax({url: url, dataType: 'json', success: resolve, error: reject});
        })
    }

    getHtml(url) {
        return new Promise((resolve, reject) => {
            $.ajax({url: url, dataType: 'html', success: resolve, error: reject});
        })
    }

    render(){
        let style = {width: '100%', height: '100%'};
        let mainContentStyle = {height: 'calc(100% - 50px)'};

        let mainContent;

        if(this.state.selectedRoom){
            mainContent = e(Musicroom, Object.assign({}, this.state.selectedRoom));
        }else{
            mainContent = e(ModelCollection, {
                model: Musicroom,
                modelsProps: this.state.musicrooms
            });
        }

        return e.div({style}, [
            e(NavBar),
            e.div({style: mainContentStyle}, mainContent)
        ])
    }

    componentDidMount(){
        window.app = this;

        this.getJson('./db/musicrooms')
            .catch(function () {
                console.log("hey",arguments)
            })
            .then((roomData) => {
                let rooms = toCamelCase(roomData.data);
                console.log("musicrooms", rooms);
                this.setState({musicrooms: rooms});

                let container = document.getElementsByClassName('container')[0];
                let args = {
                    modelsProps: rooms,
                    model: Musicroom
                };


            })
    }

}