import { unzipSync as _unzipSync } from 'fflate';

export interface NBTValue {
  type: NBTTagType;
  value: unknown;
  name?: string;
}

export enum NBTTagType {
  END = 0,
  BYTE = 1,
  SHORT = 2,
  INT = 3,
  LONG = 4,
  FLOAT = 5,
  DOUBLE = 6,
  BYTE_ARRAY = 7,
  STRING = 8,
  LIST = 9,
  COMPOUND = 10,
  INT_ARRAY = 11,
  LONG_ARRAY = 12
}

export class NBTParser {
  private buffer: DataView;
  private offset: number = 0;
  private littleEndian: boolean = false;

  constructor(data: Uint8Array, littleEndian: boolean = false) {
    this.buffer = new DataView(data.buffer, data.byteOffset, data.byteLength);
    this.littleEndian = littleEndian;
  }

  parse(): NBTValue {
    const tagType = this.readByte() as NBTTagType;
    if (tagType === NBTTagType.END) {
      return { type: NBTTagType.END, value: null };
    }
    const name = this.readString();
    const value = this.readTag(tagType);
    return { type: tagType, value, name };
  }

  private readTag(type: NBTTagType): unknown {
    switch (type) {
      case NBTTagType.END:
        return null;
      case NBTTagType.BYTE:
        return this.readByte();
      case NBTTagType.SHORT:
        return this.readShort();
      case NBTTagType.INT:
        return this.readInt();
      case NBTTagType.LONG:
        return this.readLong();
      case NBTTagType.FLOAT:
        return this.readFloat();
      case NBTTagType.DOUBLE:
        return this.readDouble();
      case NBTTagType.BYTE_ARRAY:
        return this.readByteArray();
      case NBTTagType.STRING:
        return this.readString();
      case NBTTagType.LIST:
        return this.readList();
      case NBTTagType.COMPOUND:
        return this.readCompound();
      case NBTTagType.INT_ARRAY:
        return this.readIntArray();
      case NBTTagType.LONG_ARRAY:
        return this.readLongArray();
      default:
        throw new Error(`Unknown NBT tag type: ${type}`);
    }
  }

  private readByte(): number {
    return this.buffer.getInt8(this.offset++);
  }

  private readUByte(): number {
    return this.buffer.getUint8(this.offset++);
  }

  private readShort(): number {
    const value = this.buffer.getInt16(this.offset, this.littleEndian);
    this.offset += 2;
    return value;
  }

  private readInt(): number {
    const value = this.buffer.getInt32(this.offset, this.littleEndian);
    this.offset += 4;
    return value;
  }

  private readLong(): bigint {
    const high = this.buffer.getInt32(this.offset, this.littleEndian);
    const low = this.buffer.getInt32(this.offset + 4, this.littleEndian);
    this.offset += 8;
    return (BigInt(high) << 32n) | BigInt(low >>> 0);
  }

  private readFloat(): number {
    const value = this.buffer.getFloat32(this.offset, this.littleEndian);
    this.offset += 4;
    return value;
  }

  private readDouble(): number {
    const value = this.buffer.getFloat64(this.offset, this.littleEndian);
    this.offset += 8;
    return value;
  }

  private readByteArray(): number[] {
    const length = this.readInt();
    const result: number[] = [];
    for (let i = 0; i < length; i++) {
      result.push(this.readByte());
    }
    return result;
  }

  private readString(): string {
    const length = this.readUByte() << 8 | this.readUByte();
    if (length === 0) return '';
    const bytes = new Uint8Array(this.buffer.buffer, this.buffer.byteOffset + this.offset, length);
    this.offset += length;
    return new TextDecoder('utf-8').decode(bytes);
  }

  private readList(): NBTValue[] {
    const listType = this.readByte() as NBTTagType;
    const length = this.readInt();
    const result: NBTValue[] = [];
    for (let i = 0; i < length; i++) {
      result.push({ type: listType, value: this.readTag(listType) });
    }
    return result;
  }

  private readCompound(): Record<string, NBTValue> {
    const result: Record<string, NBTValue> = {};
    while (true) {
      const tagType = this.readByte() as NBTTagType;
      if (tagType === NBTTagType.END) break;
      const name = this.readString();
      const value = this.readTag(tagType);
      result[name] = { type: tagType, value };
    }
    return result;
  }

  private readIntArray(): number[] {
    const length = this.readInt();
    const result: number[] = [];
    for (let i = 0; i < length; i++) {
      result.push(this.readInt());
    }
    return result;
  }

  private readLongArray(): bigint[] {
    const length = this.readInt();
    const result: bigint[] = [];
    for (let i = 0; i < length; i++) {
      result.push(this.readLong());
    }
    return result;
  }

  getOffset(): number {
    return this.offset;
  }

  remaining(): number {
    return this.buffer.byteLength - this.offset;
  }
}

export function parseNBT(data: Uint8Array, littleEndian: boolean = false): NBTValue {
  const parser = new NBTParser(data, littleEndian);
  return parser.parse();
}

export function getNBTValue(nbt: NBTValue | undefined, path: string): unknown {
  if (!nbt) return undefined;
  
  const parts = path.split('.');
  let current: unknown = nbt.value;
  
  for (const part of parts) {
    if (current === null || current === undefined) return undefined;
    
    if (typeof current === 'object' && !Array.isArray(current)) {
      current = (current as Record<string, NBTValue>)[part]?.value;
    } else {
      return undefined;
    }
  }
  
  return current;
}

export function getNBTString(nbt: NBTValue | undefined, path: string): string | undefined {
  const value = getNBTValue(nbt, path);
  return typeof value === 'string' ? value : undefined;
}

export function getNBTInt(nbt: NBTValue | undefined, path: string): number | undefined {
  const value = getNBTValue(nbt, path);
  return typeof value === 'number' ? value : undefined;
}

export function getNBTCompound(nbt: NBTValue | undefined, path: string): Record<string, NBTValue> | undefined {
  const value = getNBTValue(nbt, path);
  return typeof value === 'object' && !Array.isArray(value) ? value as Record<string, NBTValue> : undefined;
}
