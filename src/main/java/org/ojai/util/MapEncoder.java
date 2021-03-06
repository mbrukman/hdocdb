/**
 * Copyright (c) 2015 MapR, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ojai.util;

import org.ojai.DocumentReader;
import org.ojai.DocumentReader.EventType;
import org.ojai.annotation.API;
import org.ojai.exceptions.DecodingException;
import org.ojai.exceptions.EncodingException;
import org.ojai.exceptions.TypeException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@API.Public
public class MapEncoder {

  /**
   * Encodes values from the specified DocumentReader into
   * a Java Map.
   * @param reader
   * @return a Map view of the Document
   * @throws EncodingException
   * @throws TypeException
   */
  public static Map<String, Object> encode(DocumentReader reader)
      throws EncodingException, TypeException {
    try {
      EventType event = reader.next();
      if (event != EventType.START_MAP) {
        throw new EncodingException("Expected "
            + EventType.START_MAP + ", received " + event);
      }
      return encodeMap(reader);
    } catch (DecodingException de) {
      throw new EncodingException(de);
    }
  }

  private static Map<String, Object> encodeMap(DocumentReader dr)
      throws EncodingException, TypeException {
    try {
      Map<String, Object> docMap = new LinkedHashMap<String, Object>();
      EventType event = null;
      while (true) {
        if ((event = dr.next()) == null) {
          throw new EncodingException("EOR: malformed document.");
        }
        switch (event) {
        case START_MAP:
          docMap.put(dr.getFieldName(), encodeMap(dr));
          break;
        case END_MAP: // end of current map
          return docMap;
        case START_ARRAY:
          docMap.put(dr.getFieldName(), encodeArray(dr));
          break;
        case END_ARRAY:
          throw new EncodingException("END_ARRAY was not expected in encodeMap()");
        default:
          docMap.put(dr.getFieldName(), encodeValue(dr, event));
          break;
        }
      }
    } catch (DecodingException de) {
      throw new EncodingException(de);
    }
  }

  private static List<Object> encodeArray(DocumentReader dr)
      throws EncodingException, TypeException {
    List<Object> objList = new ArrayList<Object>();
    EventType event = null;
    try {
      event = dr.next();
    } catch (DecodingException de) {
      throw new EncodingException(de);
    }
    do {
      if (event == null) {
        throw new EncodingException("Malformed document");
      }
      switch (event) {
      case END_MAP:
      case END_ARRAY:
        throw new EncodingException("Unexpected event: " + event);
      case START_MAP:
        objList.add(dr.getArrayIndex(), encodeMap(dr));
        break;
      case START_ARRAY:
        objList.add(dr.getArrayIndex(), encodeArray(dr));
        break;
      default:
        objList.add(dr.getArrayIndex(), encodeValue(dr, event));
        break;
      }
      try {
        event = dr.next();
      } catch (DecodingException de) {
        throw new EncodingException(de);
      }
    } while (event != EventType.END_ARRAY);
    return objList;
  }

  private static Object encodeValue(DocumentReader dr, EventType event)
      throws EncodingException, TypeException {
    switch(event) {
    case NULL: return null;
    case BOOLEAN: return dr.getBoolean();
    case STRING: return dr.getString();
    case BYTE: return dr.getByte();
    case SHORT: return dr.getShort();
    case INT: return dr.getInt();
    case LONG: return dr.getLong();
    case FLOAT: return dr.getFloat();
    case DOUBLE: return dr.getDouble();
    case DECIMAL: return dr.getDecimal();
    case DATE: return dr.getDate();
    case TIME: return dr.getTime();
    case TIMESTAMP: return dr.getTimestamp();
    case INTERVAL: return dr.getInterval();
    case BINARY: return dr.getBinary().duplicate();
    default:
      throw new EncodingException("Unexpected event: " + event);
    }
  }

}
