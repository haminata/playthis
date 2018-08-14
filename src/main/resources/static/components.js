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

class ModelCollection extends React.Component {

    constructor(props) {
        super(props || {})
        this.viewFormat = props.viewFormat || VIEW_FORMAT.GRID_ITEM
    }

    render() {
        let elems = _.map(this.props.modelsProps || [], (prop) => {
            let key = `${this.props.model.name}_${prop.id}`;
            let el = e(this.props.model, _.merge({key, viewFormat: this.viewFormat}, prop));

            let style = {};

            if (this.viewFormat === VIEW_FORMAT.GRID_ITEM) {
                return e('div', {key, className: 'col-md-4 col-lg-4 col-sm-6', style}, [el]);
            } else {
                return e.li({key, className: "list-group-item"}, [el])
            }

        });



        if (this.viewFormat === VIEW_FORMAT.GRID_ITEM) {
            return e.div({className: 'container'},
                e.div({className: 'row'}, elems)
            );
        }else {
            return e.ul({className: 'list-group list-group-flush'}, elems);
        }
    }

}

class Navbar extends React.Component {

    onCreateMusicroom() {
        app.setState({newMusicroom: app.state.newMusicroom || {}})
    }

    onSignIn() {
        console.log('[Navbar] sign in')
    }

    componentDidMount() {
        window.appNavbar = this
    }

    onBrandClick() {
        app.newMusicroom = null
        app.selectedRoom = null
    }

    render() {
        let children = [
            e.a({className: 'navbar-brand', href: '#', onClick: this.onBrandClick.bind(this)}, app.name),
            e.button({
                className: 'navbar-toggler',
                type: "button",
                'data-toggle': "collapse",
                'data-target': "#navbarText"
            }, [
                e.span({className: 'navbar-toggler-icon'})
            ]),
            //<button class="btn btn-outline-success my-2 my-sm-0 ml-2" data-emit-click="create_room">Create Room</button>
            e.ul({className: 'nav nav-tab navbar-nav mr-auto'}),
            e.button({
                className: 'btn btn-outline-success my-2 my-sm-0 ml-2',
                onClick: this.onCreateMusicroom.bind(this)
            }, 'Create Room'),
            e.a({
                href: '/spotify_login',
                className: 'btn btn-link my-2 my-sm-0 ml-2',
                onClick: this.onSignIn.bind(this)
            }, 'Sign In')
        ];
        return e.nav({className: 'nav navbar navbar-dark navbar-expand-sm bg-dark'}, e.div({className: 'container'}, children))
    }
}

class Toolbar extends React.Component {

    constructor(props) {
        super(props || {});
        this.state = Object.assign({placeholder: 'Search music rooms', dropdownModels: []}, this.props);
        this.input = React.createRef();
    }

    get searchPlaceholder() {
        return this.state.placeholder || ''
    }

    componentDidMount() {
        window.appToolbar = this
    }

    onChange() {
        console.log('[input]', this.input.current.value);
        if(app.musicroomView) app.musicroomView.setState({searchQuerySong: this.input.current.value})
        else app.setState({searchQueryMusicroom: this.input.current.value});
    }

    render() {
        return e.div({className: "alert alert-warning rounded-0", role: "alert"}, [
            e.div({className: 'container'}, [
                e.div({className: 'input-group input-group-lg w-100'}, [
                    e.span({className: 'input-group-prepend border-light'}, [
                        e.div({className: 'input-group-text border bg-light'}, [
                            e.i({className: 'fa fa-search'}),
                        ])
                    ]),
                    e.input({
                        type: 'text',
                        className: 'form-control py-2 border-left-0 border rounded-right bg-light',
                        placeholder: this.searchPlaceholder,
                        ref: this.input,
                        onChange: this.onChange.bind(this)
                    }),
                    e(ModelCollection, {modelsProps: this.state.dropdownModels || [], model: Song})
                ])
            ])
        ]);
    }

}
