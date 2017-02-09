#!groovy

import groovy.json.*

def parseJSON(String response, String secretName){

  def result = new JsonSlurperClassic().parseText(response)

  try {

      if (result.errors)
          error "Vault: " + result.errors[0].toString()
      else if (secretName == 'client_token' && result.auth.client_token)
          return result.auth.client_token
      else if (secretName == 'data' && result.data)
          return result.data
      else
          error "Can't retrieve secret"
  }
  catch(Exception err)
  {
      error err.toString()
  }
}

def call(String secretName, String vaultAddress = 'http://vault.default.svc.cluster.local:8200', int userInputTime = 5) {

  def username = ''
  def password = ''

  timeout(time:userInputTime, unit:'MINUTES') {

    def userInput = input(
        id: 'userInput', message: 'User/Password/Secret', parameters: [
        [$class: 'TextParameterDefinition', defaultValue: '', description: 'Username input', name: 'username'],
        [$class: 'PasswordParameterDefinition', defaultValue: '', description: 'Password input', name: 'password']
    ])

    username=userInput['username'].toString()
    password=userInput['password'].toString()
  }

  def response = httpRequest contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: '{ "password": "' + password + '" }', url: vaultAddress + "/v1/auth/userpass/login/" + username
  def vault_token = parseJSON(response.content, 'client_token').toString()
  response = httpRequest customHeaders: [[name: 'X-Vault-Token', value: vault_token]], contentType: 'APPLICATION_JSON', httpMode: 'GET', url: vaultAddress + "/v1/secret/" + secretName
  def data = parseJSON(response.content, 'data')

  return data

}
