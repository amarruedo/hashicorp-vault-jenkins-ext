#!groovy

import groovy.json.*

def parseJSON(String response, String secretName){

  def result = new JsonSlurperClassic().parseText(response)

  try {

      if (result.errors)
          error "Vault: " + result.errors[0].toString()
      else if (secretName == "client_token" && result.auth.client_token)
          return result.auth.client_token
      else if (secretName == "data" && result.data)
          return result.data
      else
          error "Can't retrieve secret"
  }
  catch(Exception err)
  {
      error err.toString()
  }
}

def call(String vaultAddress = 'http://vault.default.svc.cluster.local:8200', int userInputTime = 5, String nodeName = 'master') {

  def username = ''
  def password = ''
  def secret = ''

  timeout(time:userInputTime, unit:'MINUTES') {

    def userInput = input(
        id: 'userInput', message: 'User/Password/Secret', parameters: [
        [$class: 'TextParameterDefinition', defaultValue: '', description: 'Username input', name: 'username'],
        [$class: 'PasswordParameterDefinition', defaultValue: '', description: 'Password input', name: 'password'],
        [$class: 'TextParameterDefinition', defaultValue: '', description: 'Secret to retrieve', name: 'secret']
    ])

    username=userInput['username'].toString()
    password=userInput['password'].toString()
    secretName=userInput['secret'].toString()
  }

  node(nodeName){

      response = sh(returnStdout: true, script:"set +x; curl -s " + vaultAddress + "/v1/auth/userpass/login/" + username +" -d '{ \"password\": \"" + password + "\" }'").trim()
      def result = parseJSON(response, "client_token")
      def vault_token = result.toString()

      secrets = sh(returnStdout: true, script:"set +x; curl -s -X GET -H \"X-Vault-Token:" + vault_token + "\" " + vaultAddress + "/v1/secret/" + secretName).trim()
      result = parseJSON(secrets, "data")

      return result
  }
}
