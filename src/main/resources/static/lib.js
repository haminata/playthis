
class Deferred {

    constructor() {
        this.id = `${this.constructor._ID++}`

        this.promise = new Promise((res, rej) => {
            this.resolve = res
            this.reject = rej
        })
    }

    cancel() {
        this.reject('Cancelled!')
    }

    then(...args) {
        this._promise = (this._promise || this.promise).then(...args)
        return this
    }

    catch(...args) {
        this._promise = (this._promise || this.promise).catch(...args)
        return this
    }
}

Deferred._ID = 1

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