# 📦 Demo Smart Things - REST API

Demo API untuk Manage Vendor, User, dan Device dalam aplikasi IoT.

## Tech Stack

- Bahasa: Java
- Framework: Ratpack
- Database: MongoDB

## Instalasi

1. Clone repository
2. Masuk ke folder di Intelij Idea
3. Install dependencies Graddle
4. Jalankan
5. Aplikasi berjalan di: http://localhost:8080
6. Tidak perlu setup database karena sudah melalui cloud di mongodb Cloud

# Endpoint

## 1. Vendor APIs (for device management)

**Base Path**: `/api/vendors`

| No | Method | Endpoint | Description | Requirement |
| --- | --- | --- | --- | --- |
| 1.1 | `POST` | `/api/vendors/{vendorId}/devices` | Create a new device | 1 |
| 1.2 | `PATCH` | `/api/vendors/{vendorId}/devices/{deviceId}` | Update device info | 1 |
| 1.3 | `GET` | `/api/vendors/{vendorId}/devices` | Get all devices for the vendor | 2 |
| 1.4 | `DELETE` | `/api/vendors/{vendorId}/devices/{deviceId}` | Delete a device (only if not registered to any user) | 3 |

## 2. User APIs (for registration and device control)

**Base Path**: `/api/users`

| No | Method | Endpoint | Description | Requirement |
| --- | --- | --- | --- | --- |
| 2.1 | `POST` | `/api/users/register` | Register a new SmartThings user | 4 |
| 2.2 | `GET`  | `/api/devices/available?lang={language}` | Get all available devices translated to user’s language | 5 |
| 2.3 | `POST` | `/api/users/{userId}/devices/{deviceId}` | Register a device to a user | 6 |
| 2.4 | `DELETE` | `/api/users/{userId}/devices/{deviceId}` | Unregister a device from a user | 6 |
| 2.5 | `GET` | `/api/users/{userId}/devices?lang={language}` | Get all registered devices (translated) | 7 |
| 2.6 | `PATCH` | `/api/users/{userId}/devices/{deviceId}/control` | Change the device value | 8 |

## 3. Admin APIs

**Base Path**: `/api/admin`

| No | Method | Endpoint | Description | Requirement |
| --- | --- | --- | --- | --- |
| 3.1 | `GET` | `/api/admin/devices` | List all vendor devices with user count | 9 |
| 3.2 | `GET` | `/api/admin/users` | List all users with number of registered devices | 10 |
| 3.3 | `GET` | `/api/admin/users/{userId}` | View detailed user information | 11 |

## 4. Mock Translation Service (internal)

**Base Path**: `/api/translation`

| No  | Method | Endpoint               | Description                   | Requirement |
|-----| --- |------------------------|-------------------------------| --- |
| 4.1 | `POST` | `/api/translation`     | Translate text to one country | 12 |
| 4.2 | `POST` | `/api/translation/all` | Translate text to all country | 12 |

# Request Response

## Code

`200 OK`

`201 Created`

`400 Bad Request`

`405 Method Not Allowed`

`404 Not Found`

`422 Unprocessable Entity`

`500 Internal Server Error`

## Response

### **1. Vendor APIs (for device management)**

### **Create a new device** (`POST /api/vendors/{vendorId}/devices`)

**Request:**

```
POST /api/vendors/68162260c79282d18e69eacc/devices
Content-Type: application/json

{
  "brandName": "Bixbi",
  "deviceName": "Smart Speaker",
  "description": "A smart speaker with adjustable volume",
  "configuration": {
    "min": 0,
    "max": 100,
    "default": 50
  }
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Device created successfully",
  "data": {
	  "deviceId": "6819c4a48cd04cc7f3e02200",
	  "brandName": "Bixbi",
    "deviceName": "Smart Light Bulb",
    "description": "A smart speaker with adjustable volume",
  }
}

```

### **Update device info** (`PATCH /api/vendors/{vendorId}/devices/{deviceId}`)

**Request:**

```
PATCH /api/vendors/68162260c79282d18e69eacc/devices/368162260c79282d18e69eacc
Content-Type: application/json

{
  "brandName": "Bixbi",
  "deviceName": "Smart Speaker",
  "description": "A smart speaker with adjustable volume",
  "configuration": {
    "min": 0,
    "max": 100,
    "default": 50
  }
}

```

**Response:**

```json
{
  "status": "success",
  "message": "Device information updated successfully",
  "data": {
        "deviceId": "681a21bbdac18ad3b9a30847"
    }
}
```

### **Get all devices for the vendor** (`GET /api/vendors/{vendorId}/devices`)

**Request:**

```
GET /api/vendors/68162260c79282d18e69eacc/devices

```

**Response:**

```json
{
  "status": "success",
  "message": "List of Vendor's Device"
  "data":
	  "vendorName":"Samsung",
	  "devices":[
    {
      "deviceId": "168162480e69a9c410ed8c14c",
      "brandName": "Smart Thermostat",
      "deviceName": "Smart Bulb",
      "description": "A smart bulb with dimming capability"
      "configuration": {
		    "min": 0,
		    "max": 100,
		    "default": 50
		  },
		  "stats": {
			  "registeredUserCount": 3
			 },
			 "createdAt": "1990-05-10",
			 "updatedAt": "1990-05-10"
    }
  ]
}

```

### **Delete a device** (`DELETE /api/vendors/{vendorId}/devices/{deviceId}`)

**Request:**

```
DELETE /api/vendors/681a21c6dac18ad3b9a30848/devices/3681a21c6dac18ad3b9a30848
```

**Response:**

```json
{
    "status": "success",
    "message": "Device Deleted successfully",
    "data": {
        "deviceId": "681a21c6dac18ad3b9a30848"
    }
}

```

### **2. User APIs (for registration and device control)**

### **Register a new SmartThings user** (`POST /api/users/register`)

**Request:**

```
POST /api/users/register
Content-Type: application/json

{
  "name": "Ajie",
  "dob": "12-12-2012",
  "address": "New Road, YK",
  "country": "id"
}
```

**Response:**

```json
{
    "message": "User registered successfully",
    "status": "success",
    "data": {
        "country": "id",
        "address": "New Road, YK",
        "dob": "12-12-2012",
        "name": "Ajie",
        "userId": "681a8af7dac18ad3b9a30849"
    }
}
```

### **Get all available devices translated to user's language** (`GET /api/devices/available?lang={language}`)

**Request:**

```
GET /api/devices/available?lang=en
```

**Response:**

```json
{
  "status": "success",
  "message": "List of device",
  "data": [
		{
      "deviceId": "681a8bd4dac18ad3b9a3084a",
      "brandName": "Smart Thermostat",
      "deviceName": "Smart Bulb",
      "description": "A smart bulb with dimming capability"
    },
    {
      "deviceId": "681a8bd4dac18ad3b9a3084a",
      "brandName": "Smart Thermostat",
      "deviceName": "Smart Bulb",
      "description": "A smart bulb with dimming capability"
    }
  ]
}
```

### **Register a device to a user** (`POST /api/users/{userId}/devices/{deviceId}`)

**Request:**

```
POST /api/users/681a8af7dac18ad3b9a30849/devices/681a8af7dac18ad3b9a30849
```

**Response:**

```json
{
  "status": "success",
  "message": "Device registered to user successfully",
  "data": {
        "userId": "681a8af7dac18ad3b9a30849",
        "deviceId": "681a8bd4dac18ad3b9a3084a"
  }
}
```

### **Unregister a device from a user** (`DELETE /api/users/{userId}/devices/{deviceId}`)

**Request:**

```
DELETE /api/users/681a8af7dac18ad3b9a30849/devices/681a8af7dac18ad3b9a30849
```

**Response:**

```json
{
  "status": "success",
  "message": "Device unregistered from user successfully",
   "data": {
        "userId": "681a8af7dac18ad3b9a30849",
        "deviceId": "681a8bd4dac18ad3b9a3084a"
    }
}

```

### **Get all registered devices** (`GET /api/users/{userId}/devices?lang={language}`)

**Request:**

```
GET /api/users/681a8bd4dac18ad3b9a3084a/devices?lang=en
```

**Response:**

```json
{
  "status": "success",
  "message": "List of User's Device"
  "data": {
        "devices": [
          {
              "brandName": "Bixbi",
              "configuration": {
                  "min": 0,
                  "max": 100,
                  "value": 50
              },
              "description": "A smart speaker with adjustable volume",
              "deviceId": "681a8bd4dac18ad3b9a3084a",
              "deviceName": "Smart Speaker"
          },
          {
              "brandName": "Samsung",
              "configuration": {
                  "min": 0,
                  "max": 100,
                  "value": 50
              },
              "description": "A smart speaker with adjustable volume",
              "deviceId": "681a8bdddac18ad3b9a3084b",
              "deviceName": "Smart Lamp"
          }
      ],
      "userName": "Ajie"
  }
}
```

### **Change the device value** (`PATCH /api/users/{userId}/devices/{deviceId}/control?changeValue={value}`)

**Request:**

```
PATCH /api/users/681a8bdddac18ad3b9a3084b/devices/681a8bdddac18ad3b9a3084b/control?changeValue=86
```

**Response:**

```json
{
  "status": "success",
  "message": "Device value updated successfully",
  "data": {
	    "userId": "681a8af7dac18ad3b9a30849",
	    "deviceId": "681a8bd4dac18ad3b9a3084a"
  }
}
```

### **3. Admin APIs**

### **List all vendor devices with user count** (`GET /api/admin/devices`)

**Request:**

```
GET /api/admin/devices
```

**Response:**

```json
{
  "status": "success",
  "message": "List All Devices with user count"
  "data": [
				 {
            "createdAt": 1746595396856,
            "brandName": "Vario",
            "configuration": {
                "min": 10,
                "max": 60,
                "default": 69
            },
            "stats": {
                "registeredUserCount": 1
            },
            "vendorId": "68162260c79282d18e69eacc",
            "description": "A Dumb LED bilbkss",
            "deviceId": "681a8bd4dac18ad3b9a3084a",
            "deviceName": "Hue black Bulb",
            "updatedAt": 1746599352770
        },
  ]
}
```

### **List all users with number of registered devices** (`GET /api/admin/users`)

**Request:**

```
GET /api/admin/users
```

**Response:**

```json
{
  "status": "success",
  "message": "List of All Users wih count",
  "data": [
			  {
            "country": "id",
            "createdAt": 1746595175454,
            "address": "New Road, YK",
            "stats": {
                "registeredDeviceCount": 1
            },
            "dob": "12-12-2012",
            "name": "Ajie",
            "userId": "681a8af7dac18ad3b9a30849",
            "updatedAt": 1746600338321
        }
  ]
}
```

### **View detailed user information** (`GET /api/admin/users/{userId}`)

**Request:**

```
GET /api/admin/users/101

```

**Response:**

```json
{
  "status": "success",
  "message": "Show User Detailed Information"
  "data": {
    "userId": "681a8af7dac18ad3b9a30849",
    "name": "Ajie",
    "dob": "1990-05-10",
    "address": "123 Elm Street",
    "countrty": "id",
    "stats": {
	    registeredDeviceCount: 3
    },
    "registeredDevices": [
      {
        "deviceId": "1",
        "value": 56,
        "lastIsed":"1990-05-10"
        "registeredAt":"1990-05-10"
      },
      {
        "deviceId": "2",
        "deviceName": "Smart Door Lock",
        "deviceType": "Lock"
      },
      {
        "deviceId": "3",
        "deviceName": "Smart Light Bulb",
        "deviceType": "Light"
      }
    ]
  }
}

{
    "data": {
        "country": "id",
        "address": "New Road, YK",
        "stats": {
            "registeredDeviceCount": 1
        },
        "dob": "12-12-2012",
        "name": "Ajie",
        "registeredDevices": [
            {
                "deviceId": {
                    "timestamp": 1746570205,
                    "date": 1746570205000
                },
                "value": 69,
                "lastUsed": 1746600338320,
                "registeredAt": 1746600338321
            }
        ],
        "userId": "681a8af7dac18ad3b9a30849"
    },
    "message": "Show User Detailed Information",
    "status": "success"
}
```

### **4. Mock Translation Service (internal)**

### **Translate text to one country** (`POST /api/translation`)

**Request:**

```
POST /api/translation
Content-Type: application/json

{
    "text":"This text is in the Language you understand",
    "country":"id"
}

```

**Response:**

```json
{
    "translation": "Text ini di dalam bahasa yang kamu pahami",
    "language": "id",
    "text": "This text is in the Language you understand"
}
```

### **Translate text to all country** (`POST /api/translation/all`)

**Request:**

```
POST /api/translation
Content-Type: application/json

{
    "text":"This text is in the Language you understand"
}

```

**Response:**

```json
{
    "translations": [
        {
            "translation": "Teks ini dalam bahasa yang kamu pahami",
            "language": "id"
        },
        {
            "translation": "Ce texte est dans une langue que vous comprenez",
            "language": "fr"
        },
        {
            "translation": "Dieser Text ist in einer Sprache, die Sie verstehen",
            "language": "de"
        },
        {
            "translation": "Este texto está en un idioma que entiendes",
            "language": "es"
        },
        {
            "translation": "这段文字是你能理解的语言",
            "language": "zh"
        },
        {
            "translation": "هذا النص بلغة تفهمها",
            "language": "ar"
        },
        {
            "translation": "Этот текст на языке, который вы понимаете",
            "language": "ru"
        },
        {
            "translation": "このテキストはあなたが理解できる言語です",
            "language": "ja"
        },
        {
            "translation": "यह पाठ उस भाषा में है जिसे आप समझते हैं",
            "language": "hi"
        },
        {
            "translation": "Este texto está em um idioma que você entende",
            "language": "pt"
        }
    ],
    "text": "This text is in the Language you understand"
}
```

# Database

## Device
#### Document Structure:

```json
{
  "vendorId": {
    "$oid": "ObjectId"
  },
  "brandName": "string",
  "deviceName": "string",
  "description": "string",
  "configuration": {
    "min": "number",
    "max": "number",
    "default": "number"
  },
  "stats": {
    "registeredUserCount": "number"
  },
  "translations": {
    // Optional: Translations of the device details in multiple languages
  },
  "createdAt": {
    "$date": "ISODate"  
  },
  "updatedAt": {
    "$date": "ISODate"  
  }
}
```

## User
#### Document Structure:

```json
{
  "_id": {
    "$oid": "ObjectId"  
  },
  "name": "string",  
  "dob": "string",  
  "address": "string",  
  "country": "string", 
  "registeredDevices": [
    {
      "deviceId": {
        "$oid": "ObjectId"  
      },
      "value": "number",  
      "lastUsed": {
        "$date": "ISODate"  
      },
      "registeredAt": {
        "$date": "ISODate"  
      }
    }
  ],
  "stats": {
    "registeredDeviceCount": "number"  
  },
  "createdAt": {
    "$date": "ISODate"  
  },
  "updatedAt": {
    "$date": "ISODate"  
  }
}
```

## Vendor
#### Document Structure:

```json
{
  "_id": {
    "$oid": "ObjectId"  
  },
  "name": "string"  
}
```

# Improvement
## API 1.2

- Error code false 500. should be 400
- Default Value can still be outside range, Vendor can change max/min value that makes the default outside of range

## API 1.4

- Error code false 500. should be 400

## API 3.3

- Serialize device id