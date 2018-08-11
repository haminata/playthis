console.log ("Hello world");

// function DbModel(modelName, schema){
//     this.typeName = modelName;
//     this.schema = schema;
//
//     if (!(this instanceof DbModel)) {
//         throw new Error("DbModel should be created with `new`")
//     }
// }
//
// DbModel.prototype.rendered = function (format) {
//     return this.getTemplate(format)
//         .then((tmpl) => {
//             return _.template(tmpl.template)(this);
//         })
// };
//
// DbModel.prototype.getTemplate = function (format) {
//     format = format || '';
//     return app.getJson(`templates/${this.typeName}${format === '' ? '' : '.'}${format}`)
// };
//
// DbModel.findOne = function (modelName, opts) {
//    return
// };
//
// DbModel.createClass = function(modelName, schema){
//
//     function Model(props) {
//         DbModel.call(this, modelName, schema);
//
//         _.each(schema.properties, (s, propName) => {
//             this[propName] = null;
//         });
//
//         _.each(props || {}, (value, attrName) => {
//             this[attrName] = value;
//         })
//     }
//
//     Model.prototype = Object.create(DbModel.prototype);
//
//     const inst = new Model();
//     Model.getTemplate = function () {
//         return inst.getTemplate();
//     };
//
//     Model.rendered = function (state) {
//         return inst.getTemplate()
//             .then((template) => {
//                 return _.template(template)(_.merge({}, state || {}, {modelName, schema}))
//             });
//     };
//
//     Object.defineProperty(Model, 'name', {value: modelName, writable: false});
//     return Model;
// };


// class PlayThisApp extends window.EventEmitter {
//
//     constructor() {
//         super();
//         this._modelSchemas = null;
//         this.models = {};
//     }
//
//     getSchemas(){
//         if(_.isPlainObject(this._modelSchemas)) Promise.resolve(this._modelSchemas);
//
//         return this.getJson('/schemas')
//             .then((res) => {
//                 console.log('[getSchemas]', res);
//
//                 _.each(res, (schema, modelName) => {
//                     this.models[modelName] = DbModel.setSchema(modelName, schema);
//                 })
//             })
//     }
//
//     getJson(url) {
//         return new Promise((resolve, reject) => {
//             $.ajax({url: url, dataType: 'json', success: resolve, error: reject});
//         })
//     }
//
//     getHtml(url) {
//         return new Promise((resolve, reject) => {
//             $.ajax({url: url, dataType: 'html', success: resolve, error: reject});
//         })
//     }
//
//     init(){
//         const emitWrap = (event) => {
//
//             let emit = event.target.getAttribute(`data-emit-${event.type}`);
//             emit = `${event.type}_${emit}`;
//             if (this.debugMode) console.log('[Directive] emitting:', emit);
//             this.emit(emit, event)
//         };
//
//         ['keyup', 'click', 'change', 'focus'].forEach((eventType) => {
//             $(document).on(eventType, `[data-emit-${eventType}]`, emitWrap)
//         });
//
//         this.getSchemas();
//
//         // return this.getJson('./musicrooms')
//         //     .catch(function () {
//         //         console.log("hey",arguments)
//         //     })
//         //     .then((roomData) => {
//         //
//         //
//         //         let rooms = toCamelCase(roomData.data);
//         //         console.log("musicrooms", rooms);
//         //
//         //         let container = document.getElementsByClassName('container')[0];
//         //         let args = {
//         //             modelsProps: rooms,
//         //             model: Musicroom
//         //         };
//         //
//         //         ReactDOM.render(e(ModelCollection, args), container, function(){
//         //             console.log('[react] rendered:', arguments);
//         //         })
//         //     })
//     }
// }

function toCamelCase(obj){
    if (_.isArray(obj)) return _.map(obj, toCamelCase);

    return _.transform(obj, function(result, value, key) {
        let newKey = _.camelCase(key);
        if(_.isPlainObject(value)){
            value = toCamelCase(value);
        }
        result[newKey] = value;
    }, {})
}
//
// app = new PlayThisApp();
//
// app.on('click_create_room', (e) => {
//    console.log('[click_create_room]', e);
//     let modalElem = $('#crud');
//
//     new app.models.Musicroom()
//         .rendered('edit')
//         .then((txt) => {
//
//             let container = modalElem.find('.modal-body').first();
//             container.empty();
//             container.append(txt);
//         });
//
//     $('#crud').modal('show');
// });
//
//
// $(function(){
//     app.init();
// });
//
// app.debugMode = true;