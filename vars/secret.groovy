#!groovy

import groovy.json.*
import java.net.URLEncoder

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

def urlEncodeSecret(String secretPath){
  def encodedList = []
  for (String text : secretPath.split("/")) {
    encodedList.add(URLEncoder.encode(text, "UTF-8"))
  }
  return encodedList.join("/").replaceAll("\\+", "%20")
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

  def response = httpRequest contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: '{ "password": "' + password + '" }', url: vaultAddress + "/v1/auth/userpass/login/" + URLEncoder.encode(username, "UTF-8")
  def vault_token = parseJSON(response.content, 'client_token').toString()
  response = httpRequest customHeaders: [[name: 'X-Vault-Token', value: vault_token, maskValue: true]], contentType: 'APPLICATION_JSON', httpMode: 'GET', url: vaultAddress + "/v1/secret" + urlEncodeSecret(secretName)
  def data = parseJSON(response.content, 'data')

  return data

}
