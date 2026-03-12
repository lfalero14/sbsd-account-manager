def accountIdentification = exchange.getProperty("accountIdentification", String.class) ?: ""
def accountType = exchange.getProperty("accountTypeFromApi", String.class) ?: ""
def accountStatus = exchange.getProperty("accountStatus", String.class) ?: ""
def partyIdentification = exchange.getProperty("partyIdentification", String.class) ?: ""
def partyName = exchange.getProperty("partyName", String.class) ?: ""
def partyTelephone = exchange.getProperty("partyTelephone", String.class) ?: ""
def partyEmail = exchange.getProperty("partyEmail", String.class) ?: ""

def result = [
  Account: [
    AccountIdentification: accountIdentification,
    AccountType: accountType,
    AccountStatus: accountStatus
  ],
  Party: [
    PartyIdentification: partyIdentification,
    PartyName: partyName,
    ContactPoint: [
      Telephone: partyTelephone,
      Email: partyEmail
    ]
  ]
]

exchange.getMessage().setBody(result)