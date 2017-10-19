var express = require('express');
var graphqlHTTP = require('express-graphql');
var graphql = require('graphql');
var fetch = require('node-fetch');
var Rx = require('rxjs');

// Maps id to User object
var fakeDatabase = {
  'a': {
    id: 'a',
    name: 'alice',
  },
  'b': {
    id: 'b',
    name: 'bob',
  },
};

const PersonType = new graphql.GraphQLObjectType({
  name: 'Person',
  description: 'Somebody that you used to know',
  fields: () => ({
    firstName: {
      type: graphql.GraphQLString,
      resolve: person => person.first_name,
    },
    lastName: {
      type: graphql.GraphQLString,
      resolve: person => person.last_name,
    },
    email: {type: graphql.GraphQLString},
    id: {type: graphql.GraphQLString},
    username: {type: graphql.GraphQLString},
    friends: {
      type: new graphql.GraphQLList(PersonType),
      resolve: person => person.friends.map(getPersonByURL),
    },
  }),
});

const BASE_URL = 'https://myapp.com/';

function fetchResponseByURL(relativeURL) {
  return fetch(`${BASE_URL}${relativeURL}`).then(res => res.json());
}

function fetchPeople() {
  return fetchResponseByURL('/people/').then(json => json.people);
}

function fetchPersonByURL(relativeURL) {
  return fetchResponseByURL(relativeURL).then(json => json.person);
}

// Define the User type
const userType = new graphql.GraphQLObjectType({
  name: 'User',
  fields: {
    id: { type: graphql.GraphQLString },
    name: { type: graphql.GraphQLString },
  }
});

// Define the Query type
const queryType = new graphql.GraphQLObjectType({
  name: 'Query',
  fields: {
    allPeople: {
  type: new graphql.GraphQLList(PersonType),
  resolve: fetchPeople,
  },
  person: {
    type: PersonType,
    args: {
      id: { type: graphql.GraphQLString },
    },
    resolve: (root, args) => fetchPersonByURL(`/people/${args.id}/`),
  },
    user: {
      type: userType,
      // `args` describes the arguments that the `user` query accepts
      args: {
        id: { type: graphql.GraphQLString }
      },
      resolve: function (_, {id}) {
        return fakeDatabase[id];
      }
    },
    rollDice : {
      type :  new graphql.GraphQLList(graphql.GraphQLInt),
      // `args` describes the arguments that the `user` query accepts
      args:  {
        numDice: {type: new graphql.GraphQLNonNull(graphql.GraphQLInt)},
        numSides: {type: graphql.GraphQLInt}
      },
      resolve: function (_, {numDice, numSides}) {
        var output = [];
        for (var i = 0; i < numDice; i++) {
          output.push(1 + Math.floor(Math.random() * (numSides || 6)));
        }
        return output;
      }
    }
  }
});

var schema = new graphql.GraphQLSchema({query: queryType});

var app = express();

let subject = new Rx.Subject();

app.use('/graphql', graphqlHTTP({
  schema: schema,
  graphiql: true,
}));

app.get('/', (req, res) => subject.next([req, res]));
subject
  .do(a => console.log('123345'))
  .subscribe(args => {
    let [req, res] = args;
    res.send('Hello World!');
  });

app.listen(4000);
console.log('Running a GraphQL API server at localhost:4000/graphql');
